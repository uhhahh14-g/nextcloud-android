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
import java.net.URI

class DailyBibleSyncPolicyTest {
    @Test
    fun onlyTheConfiguredHttpsServerIsEligible() {
        assertTrue(DailyBibleSyncPolicy.supports(URI("https://nextcloud.01084949031.kr")))
        assertFalse(DailyBibleSyncPolicy.supports(URI("http://nextcloud.01084949031.kr")))
        assertFalse(DailyBibleSyncPolicy.supports(URI("https://another.example")))
    }

    @Test
    fun missingLocalAudioIsDownloadedEvenWhenMetadataWasBrowsed() {
        assertTrue(DailyBibleSyncPolicy.needsDownload(null, 500, "same", "same"))
    }

    @Test
    fun equalLengthWithChangedContentIsDownloaded() {
        assertTrue(DailyBibleSyncPolicy.needsDownload(500, 500, "yesterday", "today"))
    }

    @Test
    fun truncatedLocalAudioIsDownloadedAgain() {
        assertTrue(DailyBibleSyncPolicy.needsDownload(200, 500, "same", "same"))
    }

    @Test
    fun unchangedDownloadedAudioDoesNotConsumeDataAgain() {
        assertFalse(DailyBibleSyncPolicy.needsDownload(500, 500, "same", "same"))
    }

    @Test
    fun missingRemoteEtagDoesNotTreatAudioAsUpToDate() {
        assertTrue(DailyBibleSyncPolicy.needsDownload(500, 500, null, null))
    }
}
