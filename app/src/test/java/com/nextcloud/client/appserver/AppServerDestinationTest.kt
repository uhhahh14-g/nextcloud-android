/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppServerDestinationTest {
    @Test
    fun allDestinationsUseTheAppServerHttpsOrigin() {
        AppServerDestination.entries.forEach {
            assertEquals("https", it.uri.scheme)
            assertEquals("nextcloud.01084949031.kr", it.uri.host)
            assertEquals(-1, it.uri.port)
            assertNull(it.uri.userInfo)
            assertNull(it.uri.query)
            assertNull(it.uri.fragment)
        }
    }

    @Test
    fun meetingUsesTheSharedMeetingApp() {
        assertEquals("/apps/wol_jw_org/", AppServerDestination.MEETING.uri.path)
    }

    @Test
    fun researchUsesTheAccountIsolatedResearchApp() {
        assertEquals("/apps/wol_research/", AppServerDestination.RESEARCH.uri.path)
    }

    @Test
    fun dailyUsesTheExistingDailyBibleApp() {
        assertEquals("/apps/daily_bible/", AppServerDestination.DAILY.uri.path)
    }
}
