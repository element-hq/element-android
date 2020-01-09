/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.network

import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.di.SessionId
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class AccessTokenInterceptor @Inject constructor(
        @SessionId private val sessionId: String,
        private val sessionParamsStore: SessionParamsStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        accessToken?.let {
            val newRequestBuilder = request.newBuilder()
            // Add the access token to all requests if it is set
            newRequestBuilder.addHeader(HttpHeaders.Authorization, "Bearer $it")
            request = newRequestBuilder.build()
        }

        return chain.proceed(request)
    }

    private val accessToken
        get() = sessionParamsStore.get(sessionId)?.credentials?.accessToken
}
