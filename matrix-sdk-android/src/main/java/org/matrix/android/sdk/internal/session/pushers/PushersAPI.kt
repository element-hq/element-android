/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface PushersAPI {

    /**
     * Get the pushers for this user.
     *
     * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushers
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers")
    suspend fun getPushers(): GetPushersResponse

    /**
     * This endpoint allows the creation, modification and deletion of pushers for this user ID.
     * The behaviour of this endpoint varies depending on the values in the JSON body.
     *
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-pushers-set
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers/set")
    suspend fun setPusher(@Body jsonPusher: JsonPusher)
}
