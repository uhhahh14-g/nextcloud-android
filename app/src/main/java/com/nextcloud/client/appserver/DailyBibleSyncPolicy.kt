/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import java.net.URI

object DailyBibleSyncPolicy {
    const val AUDIO_PATH = "/daily-bible/audio/daily-bible-review.mp3"

    fun supports(server: URI): Boolean = server.scheme == "https" &&
        server.host == "nextcloud.01084949031.kr"

    fun needsDownload(localBytes: Long?, remoteBytes: Long, localEtag: String?, remoteEtag: String?): Boolean =
        localBytes == null || localBytes != remoteBytes || remoteEtag.isNullOrBlank() || localEtag != remoteEtag
}
