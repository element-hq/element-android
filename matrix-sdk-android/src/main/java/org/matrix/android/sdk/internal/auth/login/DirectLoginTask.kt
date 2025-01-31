/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.network.httpclient.addSocketFactory
import org.matrix.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DirectLoginTask : Task<DirectLoginTask.Params, Session> {
    data class Params(
            val homeServerConnectionConfig: HomeServerConnectionConfig,
            val userId: String,
            val password: String,
            val deviceName: String,
            val deviceId: String?
    )
}

internal class DefaultDirectLoginTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionCreator: SessionCreator
) : DirectLoginTask {

    override suspend fun execute(params: DirectLoginTask.Params): Session {
        val client = buildClient(params.homeServerConnectionConfig)
        val homeServerUrl = params.homeServerConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        val loginParams = PasswordLoginParams.userIdentifier(
                user = params.userId,
                password = params.password,
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

        return sessionCreator.createSession(credentials, params.homeServerConnectionConfig, LoginType.DIRECT)
    }

    private fun buildClient(homeServerConnectionConfig: HomeServerConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(homeServerConnectionConfig)
                .build()
    }
}
