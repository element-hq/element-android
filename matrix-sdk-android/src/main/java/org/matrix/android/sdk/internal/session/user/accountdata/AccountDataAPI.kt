/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface AccountDataAPI {

    /**
     * Set some account_data for the client.
     *
     * @param userId the user id
     * @param type the type
     * @param params the put params
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/account_data/{type}")
    suspend fun setAccountData(
            @Path("userId") userId: String,
            @Path("type") type: String,
            @Body params: Any
    )
}
