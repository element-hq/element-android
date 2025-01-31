/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.common

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.matrix.android.sdk.internal.session.TestInterceptor
import javax.net.ssl.HttpsURLConnection

/**
 * Allows to intercept network requests for test purpose by
 *   - re-writing the response
 *   - changing the response code (200/404/etc..).
 *   - Test delays..
 *
 * Basic usage:
 * <code>
 *     val mockInterceptor = MockOkHttpInterceptor()
 *      mockInterceptor.addRule(MockOkHttpInterceptor.SimpleRule(".well-known/matrix/client", 200, "{}"))
 *
 *      RestHttpClientFactoryProvider.defaultProvider = RestClientHttpClientFactory(mockInterceptor)
 *      AutoDiscovery().findClientConfig("matrix.org", <callback>)
 * </code>
 */
class MockOkHttpInterceptor : TestInterceptor {

    private var rules: ArrayList<Rule> = ArrayList()

    fun addRule(rule: Rule) {
        rules.add(rule)
    }

    fun clearRules() {
        rules.clear()
    }

    override var sessionId: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        rules.forEach { rule ->
            if (originalRequest.url.toString().contains(rule.match)) {
                rule.process(originalRequest)?.let {
                    return it
                }
            }
        }

        return chain.proceed(originalRequest)
    }

    abstract class Rule(val match: String) {
        abstract fun process(originalRequest: Request): Response?
    }

    /**
     * Simple rule that reply with the given body for any request that matches the match param
     */
    class SimpleRule(
            match: String,
            private val code: Int = HttpsURLConnection.HTTP_OK,
            private val body: String = "{}"
    ) : Rule(match) {

        override fun process(originalRequest: Request): Response? {
            return Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(originalRequest)
                    .message("mocked answer")
                    .body(body.toResponseBody(null))
                    .code(code)
                    .build()
        }
    }
}
