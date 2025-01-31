/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

internal interface ThirdPartyAPI {

    /**
     * Get the third party server protocols.
     *
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1.html#get-matrix-client-r0-thirdparty-protocols
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols")
    suspend fun thirdPartyProtocols(): Map<String, ThirdPartyProtocol>

    /**
     * Retrieve a Matrix User ID linked to a user on the third party service, given a set of user parameters.
     *
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-client-r0-thirdparty-user-protocol
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/user/{protocol}")
    suspend fun getThirdPartyUser(
            @Path("protocol") protocol: String,
            @QueryMap params: Map<String, String>?
    ): List<ThirdPartyUser>
}
