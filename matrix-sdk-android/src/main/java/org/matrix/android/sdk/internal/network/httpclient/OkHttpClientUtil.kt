/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.httpclient

import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixConfiguration
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

internal fun OkHttpClient.Builder.applyMatrixConfiguration(matrixConfiguration: MatrixConfiguration): OkHttpClient.Builder {
    matrixConfiguration.proxy?.let {
        proxy(it)
    }

    // Move networkInterceptors provided in the configuration after all the others
    interceptors().removeAll(matrixConfiguration.networkInterceptors)
    matrixConfiguration.networkInterceptors.forEach {
        addInterceptor(it)
    }

    return this
}
