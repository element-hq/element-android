/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isTokenUnknownError
import org.matrix.android.sdk.internal.network.token.AccessTokenProvider

internal class AccessTokenInterceptor(
        private val accessTokenProvider: AccessTokenProvider,
        private val globalErrorReceiver: GlobalErrorReceiver,
        ) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Attempt to get the latest access token before the request token might be expiring soon.
        val response = attemptRequestWithLatestToken(chain)

        if (!accessTokenProvider.supportsRefreshTokens) {
            return response
        }

        val serverError = response.peekFailure(globalErrorReceiver) as? Failure.ServerError

        if (serverError == null || !serverError.isTokenUnknownError()) {
            return response
        }
        // Server is the source of truth on token validity, if it is no longer valid we should refresh and retry the original request.
        return attemptRequestWithLatestToken(chain, serverError)
    }

    private fun attemptRequestWithLatestToken(chain: Interceptor.Chain, serverError: Failure.ServerError? = null): Response {
        var request = chain.request()
        runBlocking {
            accessTokenProvider.getToken(serverError)?.let { token ->
                val newRequestBuilder = request.newBuilder()
                newRequestBuilder.header(HttpHeaders.Authorization, "Bearer $token")
                request = newRequestBuilder.build()
            }
        }
        return chain.proceed(request)
    }
}
