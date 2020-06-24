/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.call

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.api.session.room.call.RoomCallService
import im.vector.matrix.android.internal.session.room.RoomGetter

internal class DefaultRoomCallService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val roomGetter: RoomGetter
) : RoomCallService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): RoomCallService
    }

    override fun canStartCall(): Boolean {
        return roomGetter.getRoom(roomId)?.roomSummary()?.canStartCall.orFalse()
    }
}
