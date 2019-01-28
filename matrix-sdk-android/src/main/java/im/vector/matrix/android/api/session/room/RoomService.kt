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
import im.vector.matrix.android.api.session.room.model.RoomSummary

/**
 * This interface defines methods to get rooms. It's implemented at the session level.
 */
interface RoomService {

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