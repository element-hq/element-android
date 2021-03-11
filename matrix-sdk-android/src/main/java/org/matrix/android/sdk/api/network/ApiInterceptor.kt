/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor class for provided api paths.
 */
@Singleton
class ApiInterceptor @Inject constructor() : Interceptor {

    interface Listener {
        fun onApiResponse(path: ApiPath, response: String)
    }

    init {
        Timber.d("ApiInterceptor.init")
    }

    private val apiResponseListenersMap = mutableMapOf<ApiPath, MutableList<Listener>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.replaceFirst("/", "")
        val method = request.method

        val response = chain.proceed(request)

        findApiPath(path, method)?.let { apiPath ->
            response.peekBody(Long.MAX_VALUE).string().let {
                apiResponseListenersMap[apiPath]?.forEach { listener ->
                    listener.onApiResponse(apiPath, it)
                }
            }
        }

        return response
    }

    private fun findApiPath(path: String, method: String): ApiPath? {
        return apiResponseListenersMap
                .keys
                .find { apiPath ->
                    apiPath.method === method && isTheSamePath(apiPath.path, path)
                }
    }

    private fun isTheSamePath(pattern: String, path: String): Boolean {
        val patternSegments = pattern.split("/")
        val pathSegments = path.split("/")

        if (patternSegments.size != pathSegments.size) return false

        for (i in patternSegments.indices) {
            if (patternSegments[i] != pathSegments[i] && !patternSegments[i].startsWith("{")) {
                return false
            }
        }
        return true
    }

    /**
     * Adds listener to send intercepted api responses through.
     */
    fun addListener(path: ApiPath, listener: Listener) {
        if (!apiResponseListenersMap.contains(path)) {
            apiResponseListenersMap[path] = mutableListOf()
        }
        apiResponseListenersMap[path]?.add(listener)
    }
}
