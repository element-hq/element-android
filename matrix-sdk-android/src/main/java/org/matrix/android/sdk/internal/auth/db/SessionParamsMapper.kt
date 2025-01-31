/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
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
        return SessionParams(credentials, homeServerConnectionConfig, entity.isTokenValid, LoginType.fromName(entity.loginType))
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
        return SessionParamsEntity(
                sessionParams.credentials.sessionId(),
                sessionParams.userId,
                credentialsJson,
                homeServerConnectionConfigJson,
                sessionParams.isTokenValid,
                sessionParams.loginType.name,
        )
    }
}
