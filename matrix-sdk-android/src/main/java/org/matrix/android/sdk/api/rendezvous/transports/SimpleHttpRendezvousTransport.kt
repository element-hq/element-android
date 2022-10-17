/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.rendezvous.transports

import kotlinx.coroutines.delay
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason
import org.matrix.android.sdk.api.rendezvous.RendezvousTransport
import org.matrix.android.sdk.api.rendezvous.model.RendezvousTransportDetails
import org.matrix.android.sdk.api.rendezvous.model.SimpleHttpRendezvousTransportDetails
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Implementation of the Simple HTTP transport MSC3886: https://github.com/matrix-org/matrix-spec-proposals/pull/3886
 */
class SimpleHttpRendezvousTransport(override var onCancelled: ((reason: RendezvousFailureReason) -> Unit)?, rendezvousUri: String?) : RendezvousTransport {
    companion object {
        private val TAG = LoggerTag(SimpleHttpRendezvousTransport::class.java.simpleName, LoggerTag.RENDEZVOUS).value
    }

    override var ready = false
    private var cancelled = false
    private var uri: String?
    private var etag: String? = null
    private var expiresAt: Date? = null

    init {
        uri = rendezvousUri
    }

    override suspend fun details(): RendezvousTransportDetails {
        val uri = uri ?: throw IllegalStateException("Rendezvous not set up")

        return SimpleHttpRendezvousTransportDetails(uri)
    }

    override suspend fun send(contentType: MediaType, data: ByteArray) {
        if (cancelled) {
            return
        }

        val method = if (uri != null) "PUT" else "POST"
        val uri = this.uri ?: throw RuntimeException("No rendezvous URI")

        val httpClient = okhttp3.OkHttpClient.Builder().build()

        val request = Request.Builder()
                .url(uri)
                .method(method, data.toRequestBody())
                .header("content-type", contentType.toString())

        etag ?.let {
            request.header("if-match", it)
        }

        val response = httpClient.newCall(request.build()).execute()

        if (response.code == 404) {
            // we set to unknown and the cancel method will rewrite the reason to expired if applicable
            cancel(RendezvousFailureReason.Unknown)
            return
        }
        etag = response.header("etag")

        Timber.tag(TAG).i("Sent data to $uri new etag $etag")

        if (method == "POST") {
            val location = response.header("location") ?: throw RuntimeException("No rendezvous URI found in response")

            response.header("expires") ?.let {
                val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
                expiresAt = format.parse(it)
            }

            // resolve location header which could be relative or absolute
            this.uri = response.request.url.toUri().resolve(location).toString()
            ready = true
        }
    }

    override suspend fun receive(): ByteArray? {
        val uri = uri ?: throw IllegalStateException("Rendezvous not set up")
        val httpClient = okhttp3.OkHttpClient.Builder().build()
        while (true) {
            if (cancelled) {
                return null
            }
            Timber.tag(TAG).i("Polling: $uri after etag $etag")
            val request = Request.Builder()
                    .url(uri)
                    .get()

            etag ?.let {
                request.header("if-none-match", it)
            }

            val response = httpClient.newCall(request.build()).execute()

            try {
                // expired
                if (response.code == 404) {
                    // we set to unknown and the cancel method will rewrite the reason to expired if applicable
                    cancel(RendezvousFailureReason.Unknown)
                    return null
                }

                // rely on server expiring the channel rather than checking ourselves

                if (response.header("content-type") != "application/json") {
                    response.header("etag")?.let {
                        etag = it
                    }
                } else if (response.code == 200) {
                    response.header("etag")?.let {
                        etag = it
                    }
                    return response.body?.bytes()
                }

                // sleep for a second before polling again
                // we rely on the server expiring the channel rather than checking it ourselves
                delay(1000)
            } finally {
                response.close()
            }
        }
    }

    override suspend fun cancel(reason: RendezvousFailureReason) {
        var mappedReason = reason
        Timber.tag(TAG).i("$expiresAt")
        if (mappedReason == RendezvousFailureReason.Unknown &&
                expiresAt != null && Date() > expiresAt
        ) {
            mappedReason = RendezvousFailureReason.Expired
        }

        cancelled = true
        ready = false
        onCancelled ?.let { it(mappedReason) }

        if (mappedReason == RendezvousFailureReason.UserDeclined) {
            uri ?.let {
                try {
                    val httpClient = okhttp3.OkHttpClient.Builder().build()
                    val request = Request.Builder()
                            .url(it)
                            .delete()
                            .build()
                    httpClient.newCall(request).execute()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed to delete channel")
                }
            }
        }
    }
}
