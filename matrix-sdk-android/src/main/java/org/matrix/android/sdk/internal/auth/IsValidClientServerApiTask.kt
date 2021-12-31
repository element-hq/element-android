/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.httpclient.addSocketFactory
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface IsValidClientServerApiTask : Task<IsValidClientServerApiTask.Params, Boolean> {
    data class Params(
            val homeServerConnectionConfig: HomeServerConnectionConfig
    )
}

internal class DefaultIsValidClientServerApiTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory
) : IsValidClientServerApiTask {

    override suspend fun execute(params: IsValidClientServerApiTask.Params): Boolean {
        val client = buildClient(params.homeServerConnectionConfig)
        val homeServerUrl = params.homeServerConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        return try {
            executeRequest(null) {
                authAPI.getLoginFlows()
            }
            // We get a response, so the API is valid
            true
        } catch (failure: Throwable) {
            if (failure is Failure.OtherServerError &&
                    failure.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // Probably not valid
                false
            } else {
                // Other error
                throw failure
            }
        }
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(homeServerConnectionConfig)
                .build()
    }
}
