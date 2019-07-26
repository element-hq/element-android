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

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams

/**
 * This interface defines methods to get rooms. It's implemented at the session level.
 */
interface RoomService {

    /**
     * Create a room
     */
    fun createRoom(createRoomParams: CreateRoomParams,
                   callback: MatrixCallback<String>)

    /**
     * Join a room by id
     * @param roomId the roomId of the room to join
     * @param viaServers the servers to attempt to join the room through. One of the servers must be participating in the room.
     */
    fun joinRoom(roomId: String,
                 viaServers: List<String> = emptyList(),
                 callback: MatrixCallback<Unit>)

    /**
     * Get a room from a roomId
     * @param roomId the roomId to look for.
     * @return a room with roomId or null
     */
    fun getRoom(roomId: String): Room?

    /**
     * Get a live list of room summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of [RoomSummary]
     */
    fun liveRoomSummaries(): LiveData<List<RoomSummary>>

}