/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.auth.wellknown

import dagger.Lazy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.auth.SessionCreator
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import okhttp3.OkHttpClient
import javax.inject.Inject

internal interface DirectLoginTask : Task<DirectLoginTask.Params, Session> {
    data class Params(
            val homeServerConnectionConfig: HomeServerConnectionConfig,
            val userId: String,
            val password: String,
            val deviceName: String
    )
}

internal class DefaultDirectLoginTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionCreator: SessionCreator
) : DirectLoginTask {

    override suspend fun execute(params: DirectLoginTask.Params): Session {
        val authAPI = retrofitFactory.create(okHttpClient, params.homeServerConnectionConfig.homeServerUri.toString())
                .create(AuthAPI::class.java)

        val loginParams = PasswordLoginParams.userIdentifier(params.userId, params.password, params.deviceName)

        val credentials = executeRequest<Credentials>(null) {
            apiCall = authAPI.login(loginParams)
        }

        return sessionCreator.createSession(credentials, params.homeServerConnectionConfig)
    }
}
