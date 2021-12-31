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

package org.matrix.android.sdk.internal.network.httpclient

import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.network.AccessTokenInterceptor
import org.matrix.android.sdk.internal.network.interceptors.CurlLoggingInterceptor
import org.matrix.android.sdk.internal.network.ssl.CertUtil
import org.matrix.android.sdk.internal.network.token.AccessTokenProvider
import timber.log.Timber

internal fun OkHttpClient.Builder.addAccessTokenInterceptor(accessTokenProvider: AccessTokenProvider): OkHttpClient.Builder {
    // Remove the previous CurlLoggingInterceptor, to add it after the accessTokenInterceptor
    val existingCurlInterceptors = interceptors().filterIsInstance<CurlLoggingInterceptor>()
    interceptors().removeAll(existingCurlInterceptors)

    addInterceptor(AccessTokenInterceptor(accessTokenProvider))

    // Re add eventually the curl logging interceptors
    existingCurlInterceptors.forEach {
        addInterceptor(it)
    }

    return this
}

internal fun OkHttpClient.Builder.addSocketFactory(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient.Builder {
    try {
        val pair = CertUtil.newPinnedSSLSocketFactory(homeServerConnectionConfig)
        sslSocketFactory(pair.sslSocketFactory, pair.x509TrustManager)
        hostnameVerifier(CertUtil.newHostnameVerifier(homeServerConnectionConfig))
        connectionSpecs(CertUtil.newConnectionSpecs(homeServerConnectionConfig))
    } catch (e: Exception) {
        Timber.e(e, "addSocketFactory failed")
    }

    return this
}
