/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.account

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AccountAPI {

    /**
     * Ask the homeserver to change the password with the provided new password.
     * @param params parameters to change password.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password")
    suspend fun changePassword(@Body params: ChangePasswordParams)

    /**
     * Deactivate the user account.
     *
     * @param params the deactivate account params
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/deactivate")
    suspend fun deactivate(@Body params: DeactivateAccountParams)
}
