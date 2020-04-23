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

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.data.sessionId
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.sqldelight.auth.SessionParamsQueries
import im.vector.matrix.sqldelight.auth.AuthDatabase
import javax.inject.Inject

internal class SqliteSessionParamsStore @Inject constructor(database: AuthDatabase,
                                                            private val mapper: SqliteSessionParamsMapper) : SessionParamsStore {

    private val sessionParamsQueries: SessionParamsQueries = database.sessionParamsQueries

    override fun get(sessionId: String): SessionParams? {
        return sessionParamsQueries.getSessionParamsWithId(sessionId) { credentials_json, home_server_connection_config_json, is_token_valid ->
            mapper.map(credentials_json, home_server_connection_config_json, is_token_valid)
        }.executeAsOneOrNull()
    }

    override fun getLast(): SessionParams? {
        return getAll().lastOrNull()
    }

    override fun getAll(): List<SessionParams> {
        return sessionParamsQueries.getAllSessionParams { credentials_json, home_server_connection_config_json, is_token_valid ->
            mapper.map(credentials_json, home_server_connection_config_json, is_token_valid)
        }.executeAsList()
    }

    override suspend fun save(sessionParams: SessionParams) {
        val sessionParamsEntity = mapper.map(sessionParams)
        if (sessionParamsEntity != null) {
            sessionParamsQueries.insert(sessionParamsEntity)
        }
    }

    override suspend fun setTokenInvalid(sessionId: String) {
        sessionParamsQueries.setTokenInvalid(sessionId)
    }

    override suspend fun updateCredentials(newCredentials: Credentials) {
        val newCredentialsJson = mapper.credentialsAdapter.toJson(newCredentials)
        sessionParamsQueries.updateCredentials(newCredentialsJson, newCredentials.sessionId())
    }

    override suspend fun delete(sessionId: String) {
        sessionParamsQueries.delete(sessionId)
    }

    override suspend fun deleteAll() {
        sessionParamsQueries.deleteAll()
    }

}
