/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import java.io.File

class DailyBibleSyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val accountManager: UserAccountManager
) : Worker(context, params) {
    override fun doWork(): Result = synchronized(syncLock) {
        var retry = false
        accountManager.allUsers.filter { !it.isAnonymous && DailyBibleSyncPolicy.supports(it.server.uri) }.forEach { user ->
            retry = !syncAudio(user) || retry
        }
        if (retry) Result.retry() else Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private fun syncAudio(user: User): Boolean {
        return try {
            val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(user.toOwnCloudAccount(), context)
            val metadata = ReadFileRemoteOperation(DailyBibleSyncPolicy.AUDIO_PATH).execute(client)
            if (metadata.code == ResultCode.FILE_NOT_FOUND) return true
            if (!metadata.isSuccess) return false
            val remote = metadata.data?.firstOrNull() as? RemoteFile ?: return false
            val manager = FileDataStorageManager(user, context.contentResolver)
            val previous = manager.getFileByPath(DailyBibleSyncPolicy.AUDIO_PATH)
            val local = previous?.storagePath?.let { File(it) }?.takeIf { it.isFile }
            val file = FileStorageUtils.fillOCFile(remote)
            if (file.fileLength <= 0) return false
            if (!DailyBibleSyncPolicy.needsDownload(local?.length(), file.fileLength, previous?.etag, file.etag)) return true
            if (previous == null) {
                manager.saveFileWithParent(file, context)
            } else {
                file.fileId = previous.fileId
                file.parentId = previous.parentId
                file.storagePath = previous.storagePath
            }
            val operation = DownloadFileOperation(user, file, context)
            val result = operation.execute(client)
            if (!result.isSuccess || File(operation.savePath).length() != file.fileLength) return false
            FileDownloadHelper.instance().saveFile(file, operation, manager)
            Log_OC.i(TAG, "Daily Bible audio synchronized")
            true
        } catch (exception: Exception) {
            Log_OC.e(TAG, "Daily Bible audio synchronization failed", exception)
            false
        }
    }

    companion object {
        private const val TAG = "DailyBibleSyncWork"
        private val syncLock = Any()
    }
}
