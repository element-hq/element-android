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
