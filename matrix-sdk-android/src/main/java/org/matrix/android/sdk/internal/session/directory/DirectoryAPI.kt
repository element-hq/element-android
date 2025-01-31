/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.directory

import org.matrix.android.sdk.api.session.room.alias.RoomAliasDescription
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.room.alias.AddRoomAliasBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface DirectoryAPI {
    /**
     * Get the room ID associated to the room alias.
     *
     * @param roomAlias the room alias.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun getRoomIdByAlias(@Path("roomAlias") roomAlias: String): RoomAliasDescription

    /**
     * Get the room directory visibility.
     *
     * @param roomId the room id.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}")
    suspend fun getRoomDirectoryVisibility(@Path("roomId") roomId: String): RoomDirectoryVisibilityJson

    /**
     * Set the room directory visibility.
     *
     * @param roomId the room id.
     * @param body the body containing the new directory visibility
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}")
    suspend fun setRoomDirectoryVisibility(
            @Path("roomId") roomId: String,
            @Body body: RoomDirectoryVisibilityJson
    )

    /**
     * Add alias to the room.
     * @param roomAlias the room alias.
     * @param body the Json body
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun addRoomAlias(
            @Path("roomAlias") roomAlias: String,
            @Body body: AddRoomAliasBody
    )

    /**
     * Delete a room alias.
     * @param roomAlias the room alias.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun deleteRoomAlias(@Path("roomAlias") roomAlias: String)
}
