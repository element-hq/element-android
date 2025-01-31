/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.openid

import org.matrix.android.sdk.api.session.openid.OpenIdToken
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface OpenIdAPI {

    /**
     * Gets a bearer token from the homeserver that the user can
     * present to a third party in order to prove their ownership
     * of the Matrix account they are logged into.
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-user-userid-openid-request-token
     *
     * @param userId the user id
     * @param body an empty json body
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/openid/request_token")
    suspend fun openIdToken(
            @Path("userId") userId: String,
            @Body body: JsonDict = emptyMap()
    ): OpenIdToken
}
