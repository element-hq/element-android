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

package im.vector.matrix.android.internal.session.user

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.model.UserEntity

internal object UserEntityFactory {

    fun create(event: Event): UserEntity? {
        if (event.type != EventType.STATE_ROOM_MEMBER) {
            return null
        }
        val roomMember = event.content.toModel<RoomMember>() ?: return null
        return UserEntity(event.stateKey ?: "",
                          roomMember.displayName ?: "",
                          roomMember.avatarUrl ?: ""
        )
    }


}