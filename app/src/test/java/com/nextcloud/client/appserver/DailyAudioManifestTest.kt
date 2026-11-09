/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.time.LocalDate

class DailyAudioManifestTest {
    private val today = LocalDate.of(2026, 11, 10)
    private val content = ByteArray(12000) { 42 }
    private val hash = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

    @Test
    fun readyOnlyForTodayAndValidLengthAndHash() {
        assertTrue(DailyAudioManifest(today.toString(), hash, 12000).isReady(today))
        assertFalse(DailyAudioManifest(today.minusDays(1).toString(), hash, 12000).isReady(today))
        assertFalse(DailyAudioManifest(today.toString(), hash, 0).isReady(today))
        assertFalse(DailyAudioManifest(today.toString(), "bad", 12000).isReady(today))
    }

    @Test
    fun rejectsTruncatedAndSameSizeDifferentAudio() {
        val manifest = DailyAudioManifest(today.toString(), hash, 12000)
        assertTrue(manifest.verifies(content))
        assertFalse(manifest.verifies(content.copyOf(11999)))
        assertFalse(manifest.verifies(ByteArray(12000) { 43 }))
    }

    @Test
    fun onlyOwnAuthenticatedPushHostIsAccepted() {
        val topic = "/up" + "a".repeat(12)
        assertTrue(DailyBiblePushState.validEndpoint("https://ntfy.01084949031.kr" + topic))
        assertTrue(DailyBiblePushState.validEndpoint("https://ntfy.01084949031.kr" + topic + "?up=1"))
        for (url in listOf(
            "http://ntfy.01084949031.kr" + topic,
            "https://attacker.example" + topic,
            "https://ntfy.01084949031.kr:443" + topic,
            "https://user@ntfy.01084949031.kr" + topic,
            "https://ntfy.01084949031.kr" + topic + "?redirect=1",
            "https://ntfy.01084949031.kr/short"
        )) assertFalse(DailyBiblePushState.validEndpoint(url))
    }
}
