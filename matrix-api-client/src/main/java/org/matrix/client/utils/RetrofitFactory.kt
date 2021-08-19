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

package org.matrix.client.utils

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class RetrofitFactory(private val moshi: Moshi) {

    fun create(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(okHttpClient)
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
    }
}
