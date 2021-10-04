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
