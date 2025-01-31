/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import org.matrix.android.sdk.internal.di.MatrixScope
import java.io.IOException
import javax.inject.Inject

/**
 * No op interceptor
 */
@MatrixScope
internal class CurlLoggingInterceptor @Inject constructor() :
        Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
