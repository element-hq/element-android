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

package im.vector.matrix.android.internal.session.room

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomAvatarContent
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import javax.inject.Inject

internal class RoomAvatarResolver @Inject constructor(private val monarchy: Monarchy,
                                                      private val credentials: Credentials) {

    /**
     * Compute the room avatar url
     * @param roomId the roomId of the room to resolve avatar
     * @return the room avatar url, can be a fallback to a room member avatar or null
     */
    fun resolve(roomId: String): String? {
        var res: String? = null
        monarchy.doWithRealm { realm ->
            val roomName = EventEntity.where(realm, roomId, EventType.STATE_ROOM_AVATAR).prev()?.asDomain()
            res = roomName?.content.toModel<RoomAvatarContent>()?.avatarUrl
            if (!res.isNullOrEmpty()) {
                return@doWithRealm
            }
            val roomMembers = RoomMembers(realm, roomId)
            val members = roomMembers.queryRoomMembersEvent().findAll()
            // detect if it is a room with no more than 2 members (i.e. an alone or a 1:1 chat)
            if (members.size == 1) {
                res = members.firstOrNull()?.toRoomMember()?.avatarUrl
            } else if (members.size == 2) {
                val firstOtherMember = members.where().notEqualTo(EventEntityFields.STATE_KEY, credentials.userId).findFirst()
                res = firstOtherMember?.toRoomMember()?.avatarUrl
            }
        }
        return res
    }

    private fun EventEntity?.toRoomMember(): RoomMember? {
        return this?.asDomain()?.content?.toModel<RoomMember>()
    }
}
