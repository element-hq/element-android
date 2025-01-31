/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Get the specific headers to apply specific timeout
 * Inspired from https://github.com/square/retrofit/issues/2561
 */
internal class TimeOutInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val connectTimeout = request.header(CONNECT_TIMEOUT)?.let { Integer.valueOf(it) } ?: chain.connectTimeoutMillis()
        val readTimeout = request.header(READ_TIMEOUT)?.let { Integer.valueOf(it) } ?: chain.readTimeoutMillis()
        val writeTimeout = request.header(WRITE_TIMEOUT)?.let { Integer.valueOf(it) } ?: chain.writeTimeoutMillis()

        val newRequestBuilder = request.newBuilder()
                .removeHeader(CONNECT_TIMEOUT)
                .removeHeader(READ_TIMEOUT)
                .removeHeader(WRITE_TIMEOUT)

        request = newRequestBuilder.build()

        return chain
                .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .withReadTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .withWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .proceed(request)
    }

    companion object {
        // Custom header name
        const val CONNECT_TIMEOUT = "CONNECT_TIMEOUT"
        const val READ_TIMEOUT = "READ_TIMEOUT"
        const val WRITE_TIMEOUT = "WRITE_TIMEOUT"

        // 1 minute
        const val DEFAULT_LONG_TIMEOUT: Long = 60_000
    }
}
