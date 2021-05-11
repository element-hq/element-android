/*
 * Copyright (c) 2021 New Vector Ltd
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


package org.matrix.android.sdk.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Request
import javax.inject.Inject
import mobile.Mobile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

internal class LowBandwidthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (!request.url.encodedPath.contains("_matrix/client")) {
            return chain.proceed(request)
        }
        // Hit out to Go to handle this
        val res = this.doRequest(request)
        if (res != null) {
            return res
        }
        return chain.proceed(request)
    }

    private fun doRequest(request: Request): Response? {
        val method = request.method
        val url = request.url.toString()
        val token = request.headers.get("Authorization")?.removePrefix("Bearer ")
        val body = this.stringifyRequestBody(request)
        val result = Mobile.sendRequest(method, url, token, body)
        if (result == null) {
            return null
        }
        return Response.Builder()
                .request(request)
                .code(result.getCode().toInt())
                .protocol(Protocol.HTTP_1_1)
                .message(result.getBody())
                .body(result.getBody().toByteArray().toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .build()
    }

    private fun stringifyRequestBody(request: Request): String? {
        return try {
            val copy: Request = request.newBuilder().build()
            val buffer = Buffer()
            copy.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: IOException) {
            ""
        }
    }
}
