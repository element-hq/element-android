/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.user.model.SearchUsersParams
import org.matrix.android.sdk.internal.session.user.model.SearchUsersResponse
import retrofit2.http.Body
import retrofit2.http.POST

internal interface SearchUserAPI {

    /**
     * Perform a user search.
     *
     * @param searchUsersParams the search params.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user_directory/search")
    suspend fun searchUsers(@Body searchUsersParams: SearchUsersParams): SearchUsersResponse
}
