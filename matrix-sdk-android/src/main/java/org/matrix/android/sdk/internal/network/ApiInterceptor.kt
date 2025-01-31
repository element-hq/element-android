/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.internal.di.MatrixScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Interceptor class for provided api paths.
 */
@MatrixScope
internal class ApiInterceptor @Inject constructor() : Interceptor {

    init {
        Timber.d("ApiInterceptor.init")
    }

    private val apiResponseListenersMap = mutableMapOf<ApiPath, MutableList<ApiInterceptorListener>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.replaceFirst("/", "")
        val method = request.method

        val response = chain.proceed(request)

        synchronized(apiResponseListenersMap) {
            findApiPath(path, method)?.let { apiPath ->
                response.peekBody(Long.MAX_VALUE).string().let { networkResponse ->
                    apiResponseListenersMap[apiPath]?.forEach { listener ->
                        tryOrNull("Error in the implementation") {
                            listener.onApiResponse(apiPath, networkResponse)
                        }
                    }
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

        return patternSegments.indices.all { i ->
            patternSegments[i] == pathSegments[i] || patternSegments[i].startsWith("{")
        }
    }

    /**
     * Adds listener to send intercepted api responses through.
     */
    fun addListener(path: ApiPath, listener: ApiInterceptorListener) {
        synchronized(apiResponseListenersMap) {
            apiResponseListenersMap.getOrPut(path) { mutableListOf() }
                    .add(listener)
        }
    }

    /**
     * Remove listener to send intercepted api responses through.
     */
    fun removeListener(path: ApiPath, listener: ApiInterceptorListener) {
        synchronized(apiResponseListenersMap) {
            apiResponseListenersMap[path]?.remove(listener)
            if (apiResponseListenersMap[path]?.isEmpty() == true) {
                apiResponseListenersMap.remove(path)
            }
        }
    }
}
