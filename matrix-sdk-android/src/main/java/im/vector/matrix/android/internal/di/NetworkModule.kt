/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.di

import com.facebook.stetho.okhttp3.StethoInterceptor
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.internal.network.*
import im.vector.matrix.android.internal.network.interceptors.CurlLoggingInterceptor
import im.vector.matrix.android.internal.network.interceptors.FormattedJsonHttpLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okreplay.OkReplayInterceptor
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkModule {

    val definition = module {

        single {
            UserAgentHolder(get())
        }

        single {
            UserAgentInterceptor(get())
        }

        single {
            AccessTokenInterceptor(get())
        }

        single {
            val logger = FormattedJsonHttpLogger()
            val interceptor = HttpLoggingInterceptor(logger)
            interceptor.level = BuildConfig.OKHTTP_LOGGING_LEVEL
            interceptor
        }

        single {
            CurlLoggingInterceptor()
        }

        single {
            OkReplayInterceptor()
        }

        single {
            StethoInterceptor()
        }

        single {
            OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addNetworkInterceptor(get<StethoInterceptor>())
                    .addInterceptor(get<UserAgentInterceptor>())
                    .addInterceptor(get<AccessTokenInterceptor>())
                    .addInterceptor(get<HttpLoggingInterceptor>())
                    .apply {
                        if (BuildConfig.LOG_PRIVATE_DATA) {
                            addInterceptor(get<CurlLoggingInterceptor>())
                        }
                    }
                    .addInterceptor(get<OkReplayInterceptor>())
                    .build()
        }

        single {
            MoshiProvider.providesMoshi()
        }

        single {
            NetworkConnectivityChecker(get())
        }

        factory {
            Retrofit.Builder()
                    .client(get())
                    .addConverterFactory(UnitConverterFactory)
                    .addConverterFactory(MoshiConverterFactory.create(get()))
        }

    }
}