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

package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

internal interface RoomGetter {
    fun getRoom(roomId: String): Room?

    fun getDirectRoomWith(otherUserId: String): Room?
}

@SessionScope
internal class DefaultRoomGetter @Inject constructor(
        private val roomFactory: RoomFactory,
        private val sessionDatabase: SessionDatabase
) : RoomGetter {

    override fun getRoom(roomId: String): Room? {
        return createRoom(roomId)
    }

    override fun getDirectRoomWith(otherUserId: String): Room? {
        return sessionDatabase.roomQueries.getDirectRoomsWith(otherUserId, Membership.activeMemberships().map())
                .executeAsList()
                .firstOrNull()
                ?.let { roomId -> createRoom(roomId) }
    }

    private fun createRoom(roomId: String): Room? {
        val exists = sessionDatabase.roomQueries.exists(roomId).executeAsOne()
        return if (exists) {
            roomFactory.create(roomId)
        } else {
            null
        }
    }
}
