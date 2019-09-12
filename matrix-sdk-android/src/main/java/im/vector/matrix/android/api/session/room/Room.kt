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
import im.vector.matrix.android.api.session.room.crypto.RoomCryptoService
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.session.room.send.DraftService
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.session.room.timeline.TimelineService

/**
 * This interface defines methods to interact within a room.
 */
interface Room :
        TimelineService,
        SendService,
        DraftService,
        ReadService,
        MembershipService,
        StateService,
        RelationService,
        RoomCryptoService {

    /**
     * The roomId of this room
     */
    val roomId: String

    /**
     * A live [RoomSummary] associated with the room
     * You can observe this summary to get dynamic data from this room.
     */
    fun liveRoomSummary(): LiveData<RoomSummary>

    fun roomSummary(): RoomSummary?

}