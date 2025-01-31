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
import org.matrix.android.sdk.internal.network.token.AccessTokenProvider

internal class AccessTokenInterceptor(private val accessTokenProvider: AccessTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // Add the access token to all requests if it is set
        accessTokenProvider.getToken()?.let { token ->
            val newRequestBuilder = request.newBuilder()
            newRequestBuilder.header(HttpHeaders.Authorization, "Bearer $token")
            request = newRequestBuilder.build()
        }

        return chain.proceed(request)
    }
}
