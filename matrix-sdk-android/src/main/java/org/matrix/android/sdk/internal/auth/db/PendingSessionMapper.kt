/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.login.ResetPasswordData
import org.matrix.android.sdk.internal.auth.registration.ThreePidData
import javax.inject.Inject

internal class PendingSessionMapper @Inject constructor(moshi: Moshi) {

    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)
    private val resetPasswordDataAdapter = moshi.adapter(ResetPasswordData::class.java)
    private val threePidDataAdapter = moshi.adapter(ThreePidData::class.java)

    fun map(entity: PendingSessionEntity?): PendingSessionData? {
        if (entity == null) {
            return null
        }

        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(entity.homeServerConnectionConfigJson)!!
        val resetPasswordData = entity.resetPasswordDataJson?.let { resetPasswordDataAdapter.fromJson(it) }
        val threePidData = entity.currentThreePidDataJson?.let { threePidDataAdapter.fromJson(it) }

        return PendingSessionData(
                homeServerConnectionConfig = homeServerConnectionConfig,
                clientSecret = entity.clientSecret,
                sendAttempt = entity.sendAttempt,
                resetPasswordData = resetPasswordData,
                currentSession = entity.currentSession,
                isRegistrationStarted = entity.isRegistrationStarted,
                currentThreePidData = threePidData
        )
    }

    fun map(sessionData: PendingSessionData?): PendingSessionEntity? {
        if (sessionData == null) {
            return null
        }

        val homeServerConnectionConfigJson = homeServerConnectionConfigAdapter.toJson(sessionData.homeServerConnectionConfig)
        val resetPasswordDataJson = resetPasswordDataAdapter.toJson(sessionData.resetPasswordData)
        val currentThreePidDataJson = threePidDataAdapter.toJson(sessionData.currentThreePidData)

        return PendingSessionEntity(
                homeServerConnectionConfigJson = homeServerConnectionConfigJson,
                clientSecret = sessionData.clientSecret,
                sendAttempt = sessionData.sendAttempt,
                resetPasswordDataJson = resetPasswordDataJson,
                currentSession = sessionData.currentSession,
                isRegistrationStarted = sessionData.isRegistrationStarted,
                currentThreePidDataJson = currentThreePidDataJson
        )
    }
}
