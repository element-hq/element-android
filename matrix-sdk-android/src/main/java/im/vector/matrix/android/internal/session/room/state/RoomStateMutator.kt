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

package im.vector.matrix.android.internal.session.room.state

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomAvatarContent
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.getOrCreate
import io.realm.Realm

/**
 * This class is responsible for mutating the "room state" entities, so
 */
internal class RoomStateMutator(private val realm: Realm, private val roomId: String) {

    fun mutate(event: Event): Boolean {
        return when (event.type) {
            EventType.STATE_ROOM_AVATAR -> mutateRoomAvatar(event.content?.toModel())
            else                        -> false
        }
    }

    private fun mutateRoomAvatar(model: RoomAvatarContent?): Boolean {
        if (model == null) {
            return false
        }
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        roomSummaryEntity.avatarUrl = model.avatarUrl
        return true
    }

}
