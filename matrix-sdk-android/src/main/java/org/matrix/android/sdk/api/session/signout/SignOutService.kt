/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.signout

import org.matrix.android.sdk.api.auth.data.Credentials

/**
 * This interface defines a method to sign out, or to renew the token. It's implemented at the session level.
 */
interface SignOutService {

    /**
     * Ask the homeserver for a new access token.
     * The same deviceId will be used
     */
    suspend fun signInAgain(password: String)

    /**
     * Update the session with credentials received after SSO.
     */
    suspend fun updateCredentials(credentials: Credentials)

    /**
     * Sign out, and release the session, clear all the session data, including crypto data.
     * @param signOutFromHomeserver true if the sign out request has to be done
     */
    suspend fun signOut(signOutFromHomeserver: Boolean)
}
