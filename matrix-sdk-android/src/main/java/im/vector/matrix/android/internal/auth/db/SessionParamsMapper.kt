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

package im.vector.matrix.android.internal.auth.db

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import javax.inject.Inject

internal class SessionParamsMapper @Inject constructor(moshi: Moshi) {

    private val credentialsAdapter = moshi.adapter(Credentials::class.java)
    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)

    fun map(entity: SessionParamsEntity?): SessionParams? {
        if (entity == null) {
            return null
        }
        val credentials = credentialsAdapter.fromJson(entity.credentialsJson)
        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(entity.homeServerConnectionConfigJson)
        if (credentials == null || homeServerConnectionConfig == null) {
            return null
        }
        return SessionParams(credentials, homeServerConnectionConfig)
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
        return SessionParamsEntity(sessionParams.credentials.userId, credentialsJson, homeServerConnectionConfigJson)
    }


}