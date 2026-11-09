/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import java.net.URI

enum class AppServerDestination(private val appId: String) {
    MEETING("wol_jw_org"),
    RESEARCH("wol_research"),
    DAILY("daily_bible");

    val uri: URI
        get() = URI("https", "nextcloud.01084949031.kr", "/apps/$appId/", null)
}
