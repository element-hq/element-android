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

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.auth.login.ResetPasswordData
import im.vector.matrix.android.internal.auth.registration.PendingSessionData
import im.vector.matrix.android.internal.auth.registration.ThreePidData
import im.vector.matrix.sqldelight.auth.PendingSessionEntity
import javax.inject.Inject

internal class SqlitePendingSessionMapper @Inject constructor(moshi: Moshi) {

    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)
    private val resetPasswordDataAdapter = moshi.adapter(ResetPasswordData::class.java)
    private val threePidDataAdapter = moshi.adapter(ThreePidData::class.java)

    fun map(entity: PendingSessionEntity?): PendingSessionData? {
        if (entity == null) {
            return null
        }
        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(entity.home_server_connection_config_json)!!
        val resetPasswordData = entity.reset_password_data_json?.let { resetPasswordDataAdapter.fromJson(it) }
        val threePidData = entity.current_three_pid_data_json?.let { threePidDataAdapter.fromJson(it) }

        return PendingSessionData(
                homeServerConnectionConfig = homeServerConnectionConfig,
                clientSecret = entity.client_secret,
                sendAttempt = entity.send_attempts,
                resetPasswordData = resetPasswordData,
                currentSession = entity.current_session,
                isRegistrationStarted = entity.is_registration_started,
                currentThreePidData = threePidData)
    }

    fun map(sessionData: PendingSessionData?): PendingSessionEntity? {
        if (sessionData == null) {
            return null
        }
        val homeServerConnectionConfigJson = homeServerConnectionConfigAdapter.toJson(sessionData.homeServerConnectionConfig)
        val resetPasswordDataJson = resetPasswordDataAdapter.toJson(sessionData.resetPasswordData)
        val currentThreePidDataJson = threePidDataAdapter.toJson(sessionData.currentThreePidData)

        return PendingSessionEntity.Impl(
                home_server_connection_config_json = homeServerConnectionConfigJson,
                client_secret = sessionData.clientSecret,
                send_attempts = sessionData.sendAttempt,
                reset_password_data_json = resetPasswordDataJson,
                current_session = sessionData.currentSession,
                is_registration_started = sessionData.isRegistrationStarted,
                current_three_pid_data_json = currentThreePidDataJson
        )
    }
}
