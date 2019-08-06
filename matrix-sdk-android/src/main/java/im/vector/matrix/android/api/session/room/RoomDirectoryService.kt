/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsParams
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsResponse
import im.vector.matrix.android.api.session.room.model.thirdparty.ThirdPartyProtocol
import im.vector.matrix.android.api.util.Cancelable

/**
 * This interface defines methods to get and join public rooms. It's implemented at the session level.
 */
interface RoomDirectoryService {

    /**
     * Get rooms from directory
     */
    fun getPublicRooms(server: String?, publicRoomsParams: PublicRoomsParams, callback: MatrixCallback<PublicRoomsResponse>): Cancelable

    /**
     * Join a room by id
     */
    fun joinRoom(roomId: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Fetches the overall metadata about protocols supported by the homeserver.
     * Includes both the available protocols and all fields required for queries against each protocol.
     */
    fun getThirdPartyProtocol(callback: MatrixCallback<Map<String, ThirdPartyProtocol>>): Cancelable

}