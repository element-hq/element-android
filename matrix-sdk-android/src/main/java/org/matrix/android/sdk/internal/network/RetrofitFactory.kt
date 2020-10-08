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
     * Use only for authentication service
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
