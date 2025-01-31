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
import javax.inject.Inject

internal class UserAgentInterceptor @Inject constructor(private val userAgentHolder: UserAgentHolder) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newRequestBuilder = request.newBuilder()
        // Add the user agent to all requests if it is set
        userAgentHolder.userAgent
                .takeIf { it.isNotBlank() }
                ?.let {
                    newRequestBuilder.header(HttpHeaders.UserAgent, it)
                }
        request = newRequestBuilder.build()
        return chain.proceed(request)
    }
}
