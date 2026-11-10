/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import android.content.Context
import androidx.core.content.edit
import java.net.URI
import java.util.UUID

class DailyBiblePushState(context: Context) {
    private val preferences = context.getSharedPreferences("daily-bible-push", Context.MODE_PRIVATE)
    val device: String
        get() = preferences.getString("device", null) ?: UUID.randomUUID().toString().also {
            preferences.edit(commit = true) { putString("device", it) }
        }

    var endpoint: String
        get() = preferences.getString("endpoint", "").orEmpty()
        set(value) {
            preferences.edit { putString("endpoint", value) }
        }

    fun recordStatus(status: String) {
        preferences.edit {
            putString("status", status)
            putLong("updated_at", System.currentTimeMillis())
        }
    }

    companion object {
        fun validEndpoint(endpoint: String): Boolean = runCatching {
            val uri = URI(endpoint)
            uri.scheme == "https" && uri.host == "ntfy.01084949031.kr" && uri.port == -1 &&
                uri.userInfo == null && (uri.query == null || uri.query == "up=1") && uri.fragment == null &&
                uri.path.matches(Regex("/up[A-Za-z0-9_-]{12,62}"))
        }.getOrDefault(false)
    }
}
