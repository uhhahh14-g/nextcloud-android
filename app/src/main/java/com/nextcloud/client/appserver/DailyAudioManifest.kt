/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId

data class DailyAudioManifest(val date: String, val sha256: String, val bytes: Long) {
    fun isReady(today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))): Boolean =
        date == today.toString() && bytes in MIN_BYTES..MAX_BYTES && sha256.matches(Regex("[a-f0-9]{64}"))

    fun verifies(content: ByteArray): Boolean = content.size.toLong() == bytes &&
        sha256 == MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

    companion object {
        const val MIN_BYTES = 10000L
        const val MAX_BYTES = 20000000L
    }
}
