/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
     * Update the session with credentials received after SSO
     */
    suspend fun updateCredentials(credentials: Credentials)

    /**
     * Sign out, and release the session, clear all the session data, including crypto data
     * @param signOutFromHomeserver true if the sign out request has to be done
     */
    suspend fun signOut(signOutFromHomeserver: Boolean)
}
