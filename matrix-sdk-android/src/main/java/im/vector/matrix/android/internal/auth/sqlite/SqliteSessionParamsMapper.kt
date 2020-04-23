/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.internal.auth.sqlite

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.data.sessionId
import im.vector.matrix.sqldelight.auth.SessionParamsEntity
import javax.inject.Inject

internal class SqliteSessionParamsMapper @Inject constructor(moshi: Moshi) {

    val credentialsAdapter = moshi.adapter(Credentials::class.java)
    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)

    fun map(credentialsJson: String, homeServerConnectionConfigJson: String, isTokenValid: Boolean): SessionParams {
        val credentials = credentialsAdapter.fromJson(credentialsJson)
        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(homeServerConnectionConfigJson)
        if (credentials == null || homeServerConnectionConfig == null) {
            throw JsonDataException()
        }
        return SessionParams(credentials, homeServerConnectionConfig, isTokenValid)
    }

    fun map(sessionParams: SessionParams?): SessionParamsEntity? {
        if (sessionParams == null) {
            return null
        }
        val credentialsJson = credentialsAdapter.toJson(sessionParams.credentials)
        val homeServerConnectionConfigJson = homeServerConnectionConfigAdapter.toJson(sessionParams.homeServerConnectionConfig)
        if (credentialsJson == null || homeServerConnectionConfigJson == null) {
            return null
        }
        return SessionParamsEntity.Impl(
                session_id = sessionParams.credentials.sessionId(),
                user_id = sessionParams.credentials.userId,
                credentials_json = credentialsJson,
                home_server_connection_config_json = homeServerConnectionConfigJson,
                is_token_valid = sessionParams.isTokenValid)
    }



}
