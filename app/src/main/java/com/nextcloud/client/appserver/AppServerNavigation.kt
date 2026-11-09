/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils

object AppServerNavigation {
    @JvmStatic
    fun open(activity: Activity, menuItemId: Int): Boolean {
        val destination = when (menuItemId) {
            R.id.nav_appserver_meeting -> AppServerDestination.MEETING
            R.id.nav_appserver_research -> AppServerDestination.RESEARCH
            R.id.nav_appserver_daily -> AppServerDestination.DAILY
            else -> return false
        }

        try {
            // Browser cookies and passkeys stay in the browser used by Login Flow v2.
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(activity, Uri.parse(destination.uri.toString()))
        } catch (_: ActivityNotFoundException) {
            DisplayUtils.showSnackMessage(activity, R.string.authenticator_activity_no_web_browser_found)
        }
        return true
    }
}
