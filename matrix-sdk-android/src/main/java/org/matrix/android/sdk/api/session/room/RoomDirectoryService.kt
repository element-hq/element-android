/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room

import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsResponse

/**
 * This interface defines methods to get and join public rooms. It's implemented at the session level.
 */
interface RoomDirectoryService {

    /**
     * Get rooms from directory.
     */
    suspend fun getPublicRooms(
            server: String?,
            publicRoomsParams: PublicRoomsParams
    ): PublicRoomsResponse

    /**
     * Get the visibility of a room in the directory.
     */
    suspend fun getRoomDirectoryVisibility(roomId: String): RoomDirectoryVisibility

    /**
     * Set the visibility of a room in the directory.
     */
    suspend fun setRoomDirectoryVisibility(roomId: String, roomDirectoryVisibility: RoomDirectoryVisibility)

    suspend fun checkAliasAvailability(aliasLocalPart: String?): AliasAvailabilityResult
}
