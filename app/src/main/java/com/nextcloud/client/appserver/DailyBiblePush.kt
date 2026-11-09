/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import android.content.Context
import com.owncloud.android.R
import org.unifiedpush.android.connector.UnifiedPush

object DailyBiblePush {
    const val INSTANCE = "daily-bible"

    fun register(context: Context) {
        if (context.packageName != "kr.appserver210.nextcloud") return
        val distributor = UnifiedPush.getDistributors(context).firstOrNull { it.startsWith("io.heckel.ntfy") }
            ?: return
        UnifiedPush.saveDistributor(context, distributor)
        UnifiedPush.register(
            context,
            instance = INSTANCE,
            messageForDistributor = context.getString(R.string.appserver210_daily_push_name)
        )
    }
}
