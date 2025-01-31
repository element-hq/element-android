/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.download

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class DownloadProgressInterceptor @Inject constructor(
        private val downloadStateTracker: DefaultContentDownloadStateTracker
) : Interceptor {

    companion object {
        const val DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER = "matrix-sdk:mxc_URL"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.toUrl()
        val mxcURl = chain.request().header(DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER)

        val request = chain.request().newBuilder()
                .removeHeader(DOWNLOAD_PROGRESS_INTERCEPTOR_HEADER)
                .build()

        val originalResponse = chain.proceed(request)
        if (!originalResponse.isSuccessful) {
            downloadStateTracker.error(mxcURl ?: url.toExternalForm(), originalResponse.code)
            return originalResponse
        }
        val responseBody = originalResponse.body ?: return originalResponse
        return originalResponse.newBuilder()
                .body(ProgressResponseBody(responseBody, mxcURl ?: url.toExternalForm(), downloadStateTracker))
                .build()
    }
}
