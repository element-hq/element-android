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
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.internal.network.AccessTokenInterceptor
import im.vector.matrix.android.internal.network.UnitConverterFactory
import im.vector.matrix.android.internal.network.UserAgentInterceptor
import im.vector.matrix.android.internal.network.interceptors.CurlLoggingInterceptor
import im.vector.matrix.android.internal.network.interceptors.FormattedJsonHttpLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okreplay.OkReplayInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@Module
internal class NetworkModule {

    @MatrixScope
    @Provides
    fun providesHttpLogingInterceptor(): HttpLoggingInterceptor {
        val logger = FormattedJsonHttpLogger()
        val interceptor = HttpLoggingInterceptor(logger)
        interceptor.level = BuildConfig.OKHTTP_LOGGING_LEVEL
        return interceptor
    }

    @MatrixScope
    @Provides
    fun providesOkReplayInterceptor(): OkReplayInterceptor {
        return OkReplayInterceptor()
    }

    @MatrixScope
    @Provides
    fun providesStethoInterceptor(): StethoInterceptor {
        return StethoInterceptor()
    }

    @MatrixScope
    @Provides
    fun providesOkHttpClient(stethoInterceptor: StethoInterceptor,
                             userAgentInterceptor: UserAgentInterceptor,
                             accessTokenInterceptor: AccessTokenInterceptor,
                             httpLoggingInterceptor: HttpLoggingInterceptor,
                             curlLoggingInterceptor: CurlLoggingInterceptor,
                             okReplayInterceptor: OkReplayInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(stethoInterceptor)
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(accessTokenInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .apply {
                    if (BuildConfig.LOG_PRIVATE_DATA) {
                        addInterceptor(curlLoggingInterceptor)
                    }
                }
                .addInterceptor(okReplayInterceptor)
                .build()
    }

    @MatrixScope
    @Provides
    fun providesMoshi(): Moshi {
        return MoshiProvider.providesMoshi()
    }

    @Provides
    fun providesRetrofitBuilder(okHttpClient: OkHttpClient,
                                moshi: Moshi): Retrofit.Builder {
        return Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
    }

}