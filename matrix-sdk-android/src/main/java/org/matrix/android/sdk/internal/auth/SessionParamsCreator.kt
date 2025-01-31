/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth

import android.net.Uri
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.extensions.tryOrNull
import timber.log.Timber
import javax.inject.Inject

internal interface SessionParamsCreator {

    suspend fun create(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            loginType: LoginType,
    ): SessionParams
}

internal class DefaultSessionParamsCreator @Inject constructor(
        private val isValidClientServerApiTask: IsValidClientServerApiTask
) : SessionParamsCreator {

    override suspend fun create(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            loginType: LoginType,
    ) = SessionParams(
            credentials = credentials,
            homeServerConnectionConfig = homeServerConnectionConfig.overrideWithCredentials(credentials),
            isTokenValid = true,
            loginType = loginType,
    )

    private suspend fun HomeServerConnectionConfig.overrideWithCredentials(credentials: Credentials) = copy(
            homeServerUriBase = credentials.getHomeServerUri(this) ?: homeServerUriBase,
            identityServerUri = credentials.getIdentityServerUri() ?: identityServerUri
    )

    private suspend fun Credentials.getHomeServerUri(homeServerConnectionConfig: HomeServerConnectionConfig) =
            discoveryInformation?.homeServer?.baseURL
                    ?.trim { it == '/' }
                    ?.takeIf { it.isNotBlank() }
                    // It can be the same value, so in this case, do not check again the validity
                    ?.takeIf { it != homeServerConnectionConfig.homeServerUriBase.toString() }
                    ?.also { Timber.d("Overriding homeserver url to $it (will check if valid)") }
                    ?.let { Uri.parse(it) }
                    ?.takeIf { validateUri(it, homeServerConnectionConfig) }

    private suspend fun validateUri(uri: Uri, homeServerConnectionConfig: HomeServerConnectionConfig) =
            // Validate the URL, if the configuration is wrong server side, do not override
            tryOrNull {
                performClientServerApiValidation(uri, homeServerConnectionConfig)
            } ?: true // In case of other error (no network, etc.), consider it is valid...

    private suspend fun performClientServerApiValidation(uri: Uri, homeServerConnectionConfig: HomeServerConnectionConfig) =
            isValidClientServerApiTask.execute(
                    IsValidClientServerApiTask.Params(homeServerConnectionConfig.copy(homeServerUriBase = uri))
            ).also { Timber.d("Overriding homeserver url: $it") }

    private fun Credentials.getIdentityServerUri() = discoveryInformation?.identityServer?.baseURL
            ?.trim { it == '/' }
            ?.takeIf { it.isNotBlank() }
            ?.also { Timber.d("Overriding identity server url to $it") }
            ?.let { Uri.parse(it) }
}
