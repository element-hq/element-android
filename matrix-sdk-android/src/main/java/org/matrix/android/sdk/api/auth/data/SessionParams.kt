/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.data

import org.matrix.android.sdk.api.auth.LoginType

/**
 * This data class holds necessary data to open a session.
 * You don't have to manually instantiate it.
 */
data class SessionParams(
        /**
         * Please consider using shortcuts instead.
         */
        val credentials: Credentials,

        /**
         * Please consider using shortcuts instead.
         */
        val homeServerConnectionConfig: HomeServerConnectionConfig,

        /**
         * Set to false if the current token is not valid anymore. Application should not have to use this info.
         */
        val isTokenValid: Boolean,

        /**
         * The authentication method that was used to create the session.
         */
        val loginType: LoginType,
) {
    /*
     * Shortcuts. Usually the application should only need to use these shortcuts
     */

    /**
     * The userId of the session (Ex: "@user:domain.org").
     */
    val userId = credentials.userId

    /**
     * The deviceId of the session (Ex: "ABCDEFGH").
     */
    val deviceId = credentials.deviceId

    /**
     * The homeserver Url entered by the user during the login phase.
     */
    val homeServerUrl = homeServerConnectionConfig.homeServerUri.toString()

    /**
     * The current homeserver Url for client-server API. It can be different that the homeserver url entered
     * during login phase, because a redirection may have occurred
     */
    val homeServerUrlBase = homeServerConnectionConfig.homeServerUriBase.toString()

    /**
     * The current homeserver host, using what has been entered by the user during login phase.
     */
    val homeServerHost = homeServerConnectionConfig.homeServerUri.host

    /**
     * The default identity server url if any, returned by the homeserver during login phase.
     */
    val defaultIdentityServerUrl = homeServerConnectionConfig.identityServerUri?.toString()
}
