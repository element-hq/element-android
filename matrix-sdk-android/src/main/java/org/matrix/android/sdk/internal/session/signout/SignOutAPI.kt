/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.signout

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface SignOutAPI {

    /**
     * Attempt to login again to the same account.
     * Set all the timeouts to 1 minute
     * It is similar to [AuthAPI.login]
     *
     * @param loginParams the login parameters
     */
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    suspend fun loginAgain(@Body loginParams: PasswordLoginParams): Credentials

    /**
     * Invalidate the access token, so that it can no longer be used for authorization.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "logout")
    suspend fun signOut()
}
