/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth.login

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.auth.SessionCreator
import org.matrix.android.sdk.internal.auth.data.TokenLoginParams
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.httpclient.addSocketFactory
import org.matrix.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface QrLoginTokenTask : Task<QrLoginTokenTask.Params, Session> {
    data class Params(
            val homeServerConnectionConfig: HomeServerConnectionConfig,
            val loginToken: String,
            val deviceName: String?,
            val deviceId: String?
    )
}

internal class DefaultQrLoginTokenTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionCreator: SessionCreator,
) : QrLoginTokenTask {

    override suspend fun execute(params: QrLoginTokenTask.Params): Session {
        val client = buildClient(params.homeServerConnectionConfig)
        val homeServerUrl = params.homeServerConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        val loginParams = TokenLoginParams(
                token = params.loginToken,
                deviceDisplayName = params.deviceName,
                deviceId = params.deviceId
        )

        val credentials = try {
            executeRequest(null) {
                authAPI.login(loginParams)
            }
        } catch (throwable: Throwable) {
            throw when (throwable) {
                is UnrecognizedCertificateException -> Failure.UnrecognizedCertificateFailure(
                        homeServerUrl,
                        throwable.fingerprint
                )
                else -> throwable
            }
        }

        return sessionCreator.createSession(credentials, params.homeServerConnectionConfig, LoginType.QR)
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(homeServerConnectionConfig)
                .build()
    }
}
