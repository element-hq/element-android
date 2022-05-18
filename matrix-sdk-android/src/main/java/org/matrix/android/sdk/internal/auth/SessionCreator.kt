/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
