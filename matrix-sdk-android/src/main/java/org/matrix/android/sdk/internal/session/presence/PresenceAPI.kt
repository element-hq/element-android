/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.presence

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.presence.model.GetPresenceResponse
import org.matrix.android.sdk.internal.session.presence.model.SetPresenceBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface PresenceAPI {

    /**
     * Set the presence status of the current user
     * Ref: https://matrix.org/docs/spec/client_server/latest#put-matrix-client-r0-presence-userid-status
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "presence/{userId}/status")
    suspend fun setPresence(
            @Path("userId") userId: String,
            @Body body: SetPresenceBody
    )

    /**
     * Get the given user's presence state.
     * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-presence-userid-status
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "presence/{userId}/status")
    suspend fun getPresence(@Path("userId") userId: String): GetPresenceResponse
}
