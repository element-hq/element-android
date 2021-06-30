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

import android.net.Uri
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.SessionManager
import timber.log.Timber
import javax.inject.Inject

internal interface SessionCreator {
    suspend fun createSession(credentials: Credentials, homeServerConnectionConfig: HomeServerConnectionConfig): Session
}

internal class DefaultSessionCreator @Inject constructor(
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val pendingSessionStore: PendingSessionStore,
        private val isValidClientServerApiTask: IsValidClientServerApiTask
) : SessionCreator {

    /**
     * Credentials can affect the homeServerConnectionConfig, override home server url and/or
     * identity server url if provided in the credentials
     */
    override suspend fun createSession(credentials: Credentials, homeServerConnectionConfig: HomeServerConnectionConfig): Session {
        // We can cleanup the pending session params
        pendingSessionStore.delete()

        val overriddenUrl = credentials.discoveryInformation?.homeServer?.baseURL
                // remove trailing "/"
                ?.trim { it == '/' }
                ?.takeIf { it.isNotBlank() }
                ?.also { Timber.d("Overriding homeserver url to $it (will check if valid)") }
                ?.let { Uri.parse(it) }
                ?.takeIf {
                    // Validate the URL, if the configuration is wrong server side, do not override
                    tryOrNull {
                        isValidClientServerApiTask.execute(
                                IsValidClientServerApiTask.Params(
                                        homeServerConnectionConfig.copy(homeServerUriBase = it)
                                )
                        )
                                .also { Timber.d("Overriding homeserver url: $it") }
                    } ?: true // In case of other error (no network, etc.), consider it is valid...
                }

        val sessionParams = SessionParams(
                credentials = credentials,
                homeServerConnectionConfig = homeServerConnectionConfig.copy(
                        homeServerUriBase = overriddenUrl ?: homeServerConnectionConfig.homeServerUriBase,
                        identityServerUri = credentials.discoveryInformation?.identityServer?.baseURL
                                // remove trailing "/"
                                ?.trim { it == '/' }
                                ?.takeIf { it.isNotBlank() }
                                ?.also { Timber.d("Overriding identity server url to $it") }
                                ?.let { Uri.parse(it) }
                                ?: homeServerConnectionConfig.identityServerUri
                ),
                isTokenValid = true)

        sessionParamsStore.save(sessionParams)
        return sessionManager.getOrCreateSession(sessionParams)
    }
}
