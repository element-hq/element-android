/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth

import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.SessionManager
import javax.inject.Inject

internal interface SessionCreator {

    suspend fun createSession(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            loginType: LoginType,
    ): Session
}

internal class DefaultSessionCreator @Inject constructor(
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val pendingSessionStore: PendingSessionStore,
        private val sessionParamsCreator: SessionParamsCreator,
) : SessionCreator {

    /**
     * Credentials can affect the homeServerConnectionConfig, override homeserver url and/or
     * identity server url if provided in the credentials.
     */
    override suspend fun createSession(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            loginType: LoginType,
    ): Session {
        // We can cleanup the pending session params
        pendingSessionStore.delete()
        val sessionParams = sessionParamsCreator.create(credentials, homeServerConnectionConfig, loginType)
        sessionParamsStore.save(sessionParams)
        return sessionManager.getOrCreateSession(sessionParams)
    }
}
