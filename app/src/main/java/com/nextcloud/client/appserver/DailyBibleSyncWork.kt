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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DailyBibleSyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val accountManager: UserAccountManager
) : Worker(context, params) {
    override fun doWork(): Result = synchronized(syncLock) {
        var retry = false
        accountManager.allUsers
            .filter { !it.isAnonymous && DailyBibleSyncPolicy.supports(it.server.uri) }
            .forEach { user -> retry = !syncAudio(user) || retry }
        if (retry) Result.retry() else Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private fun syncAudio(user: User): Boolean {
        return try {
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(user.toOwnCloudAccount(), context)
            val api = DailyBibleApi(client, user.server.uri)
            val state = DailyBiblePushState(context)
            api.register(state)
            val manifest = api.manifest()
            if (!manifest.isReady()) return false
            val metadata = ReadFileRemoteOperation(DailyBibleSyncPolicy.AUDIO_PATH).execute(client)
            if (metadata.code == ResultCode.FILE_NOT_FOUND) return true
            if (!metadata.isSuccess) return false
            val remote = metadata.data?.firstOrNull() as? RemoteFile ?: return false
            val manager = FileDataStorageManager(user, context.contentResolver)
            val previous = manager.getFileByPath(DailyBibleSyncPolicy.AUDIO_PATH)
            val file = FileStorageUtils.fillOCFile(remote)
            val local = File(previous?.storagePath ?: FileStorageUtils.getDefaultSavePathFor(user.accountName, file))
            val existing = local.takeIf { it.isFile && it.length() == manifest.bytes }
            if (existing == null || !manifest.verifies(existing.readBytes())) {
                val content = api.audio(manifest)
                if (!manifest.verifies(content)) return false
                saveVerified(local, content)
            }
            if (previous != null) {
                file.fileId = previous.fileId
                file.parentId = previous.parentId
            }
            saveMetadata(file, local, manager)
            api.acknowledge(state, manifest.sha256)
            state.recordStatus("saved_" + manifest.date)
            Log_OC.i(TAG, "Daily Bible audio synchronized")
            true
        } catch (_: FileNotFoundException) {
            true
        } catch (exception: Exception) {
            Log_OC.e(TAG, "Daily Bible audio synchronization failed", exception)
            false
        }
    }

    private fun saveVerified(target: File, content: ByteArray) {
        val parent = checkNotNull(target.parentFile)
        check(parent.isDirectory || parent.mkdirs())
        val temporary = File.createTempFile(".daily-bible-", ".mp3", parent)
        try {
            temporary.outputStream().use { it.write(content); it.fd.sync() }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private fun saveMetadata(file: OCFile, local: File, manager: FileDataStorageManager) {
        file.storagePath = local.absolutePath
        file.fileLength = local.length()
        file.lastSyncDateForData = System.currentTimeMillis()
        file.lastSyncDateForProperties = file.lastSyncDateForData
        file.modificationTimestampAtLastSyncForData = file.modificationTimestamp
        manager.saveFileWithParent(file, context)
    }

    companion object {
        private const val TAG = "DailyBibleSyncWork"
        private val syncLock = Any()
    }
}
