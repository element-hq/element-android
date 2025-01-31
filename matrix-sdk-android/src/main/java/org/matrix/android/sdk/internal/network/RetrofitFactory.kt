/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import com.squareup.moshi.Moshi
import dagger.Lazy
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.matrix.android.sdk.internal.util.ensureTrailingSlash
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject

internal class RetrofitFactory @Inject constructor(private val moshi: Moshi) {

    /**
     * Use only for authentication service.
     */
    fun create(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(okHttpClient)
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
    }

    fun create(okHttpClient: Lazy<OkHttpClient>, baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .callFactory(object : Call.Factory {
                    override fun newCall(request: Request): Call {
                        return okHttpClient.get().newCall(request)
                    }
                })
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
    }
}
