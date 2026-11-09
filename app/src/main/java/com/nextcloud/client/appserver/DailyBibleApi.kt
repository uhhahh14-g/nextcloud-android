/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appserver

import com.owncloud.android.lib.common.OwnCloudClient
import org.apache.commons.httpclient.HttpMethodBase
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.FileNotFoundException
import java.net.URI

class DailyBibleApi(private val client: OwnCloudClient, server: URI) {
    private val base = server.toString().trimEnd('/') + "/ocs/v2.php/apps/daily_bible/api/v1/push/"

    fun manifest(): DailyAudioManifest {
        val data = getJson("manifest")
        return DailyAudioManifest(data.getString("date"), data.getString("sha256"), data.getLong("bytes"))
    }

    fun register(state: DailyBiblePushState) {
        if (!DailyBiblePushState.validEndpoint(state.endpoint)) return
        postJson("device", JSONObject().put("device", state.device).put("endpoint", state.endpoint))
    }

    fun acknowledge(state: DailyBiblePushState, sha256: String) {
        if (state.endpoint.isEmpty()) return
        postJson("saved", JSONObject().put("device", state.device).put("sha256", sha256))
    }

    fun audio(manifest: DailyAudioManifest): ByteArray = request(
        GetMethod(base + "audio?sha256=" + manifest.sha256)
    )

    private fun getJson(path: String): JSONObject = unwrap(request(GetMethod(base + path)))

    private fun postJson(path: String, body: JSONObject) {
        val method = PostMethod(base + path)
        method.requestEntity = StringRequestEntity(body.toString(), "application/json", "UTF-8")
        unwrap(request(method))
    }

    private fun unwrap(body: ByteArray): JSONObject {
        val ocs = JSONObject(body.toString(Charsets.UTF_8)).getJSONObject("ocs")
        if (ocs.getJSONObject("meta").getInt("statuscode") !in SUCCESS_CODES) throw IOException("daily_api_rejected")
        return ocs.getJSONObject("data")
    }

    private fun request(method: HttpMethodBase): ByteArray {
        method.setRequestHeader("OCS-APIRequest", "true")
        method.setRequestHeader("Accept", "application/json")
        method.followRedirects = false
        try {
            val status = client.executeMethod(method)
            if (status == HTTP_FORBIDDEN || status == HTTP_NOT_FOUND) throw FileNotFoundException("daily_not_shared")
            if (status != HTTP_OK) throw IOException("daily_api_unavailable")
            val output = ByteArrayOutputStream()
            method.responseBodyAsStream.use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var count = stream.read(buffer)
                while (count >= 0) {
                    if (output.size().toLong() + count > DailyAudioManifest.MAX_BYTES) {
                        throw IOException("daily_audio_too_large")
                    }
                    output.write(buffer, 0, count)
                    count = stream.read(buffer)
                }
            }
            return output.toByteArray()
        } finally {
            method.releaseConnection()
        }
    }

    companion object {
        private const val HTTP_OK = 200
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private val SUCCESS_CODES = setOf(100, HTTP_OK)
    }
}
