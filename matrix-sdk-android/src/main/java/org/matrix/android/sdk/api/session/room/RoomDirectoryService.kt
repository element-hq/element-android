/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Get rooms from directory
     */
    suspend fun getPublicRooms(server: String?,
                               publicRoomsParams: PublicRoomsParams): PublicRoomsResponse

    /**
     * Get the visibility of a room in the directory
     */
    suspend fun getRoomDirectoryVisibility(roomId: String): RoomDirectoryVisibility

    /**
     * Set the visibility of a room in the directory
     */
    suspend fun setRoomDirectoryVisibility(roomId: String, roomDirectoryVisibility: RoomDirectoryVisibility)

    suspend fun checkAliasAvailability(aliasLocalPart: String?) : AliasAvailabilityResult
}
