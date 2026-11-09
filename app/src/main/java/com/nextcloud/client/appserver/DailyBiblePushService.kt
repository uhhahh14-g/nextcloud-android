/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class DailyBiblePushService : PushService() {
    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        if (instance != DailyBiblePush.INSTANCE) return
        val state = DailyBiblePushState(this)
        if (!DailyBiblePushState.validEndpoint(endpoint.url)) {
            state.recordStatus("configure_self_hosted_ntfy")
            return
        }
        state.endpoint = endpoint.url
        state.recordStatus("registered")
        DailyBibleSyncScheduler.syncNow(this, replacePending = true)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        if (instance != DailyBiblePush.INSTANCE) return
        DailyBiblePushState(this).recordStatus("push_received")
        DailyBibleSyncScheduler.syncNow(this, replacePending = true)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        DailyBiblePushState(this).recordStatus("registration_failed")
    }

    override fun onUnregistered(instance: String) {
        DailyBiblePushState(this).apply {
            endpoint = ""
            recordStatus("unregistered")
        }
    }
}
