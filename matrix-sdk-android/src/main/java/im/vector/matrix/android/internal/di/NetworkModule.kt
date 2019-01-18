/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.di

import im.vector.matrix.android.internal.network.AccessTokenInterceptor
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okreplay.OkReplayInterceptor
import org.koin.dsl.module.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NetworkModule {

    val definition = module {

        single {
            AccessTokenInterceptor(get())
        }

        single {
            val logger = HttpLoggingInterceptor.Logger { message -> Timber.v(message) }
            val interceptor = HttpLoggingInterceptor(logger)
            interceptor.level = HttpLoggingInterceptor.Level.BASIC
            interceptor
        }

        single {
            OkReplayInterceptor()
        }

        single {
            OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(get<AccessTokenInterceptor>())
                    .addInterceptor(get<HttpLoggingInterceptor>())
                    .addInterceptor(get<OkReplayInterceptor>())
                    .build()
        }

        single {
            MoshiProvider.providesMoshi()
        }

        single {
            MoshiConverterFactory.create(get()) as Converter.Factory
        }

        single {
            NetworkConnectivityChecker(get())
        }

        factory {
            Retrofit.Builder()
                    .client(get())
                    .addConverterFactory(get())
        }

    }
}