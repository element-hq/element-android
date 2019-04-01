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
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomAvatarContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMembers

internal class RoomAvatarResolver(private val monarchy: Monarchy,
                                  private val credentials: Credentials) {

    /**
     * Compute the room avatar url
     * @param roomId the roomId of the room to resolve avatar
     * @return the room avatar url, can be a fallback to a room member avatar or null
     */
    fun resolve(roomId: String): String? {
        var res: String? = null
        monarchy.doWithRealm { realm ->
            val roomEntity = RoomEntity.where(realm, roomId).findFirst()
            val roomName = EventEntity.where(realm, roomId, EventType.STATE_ROOM_AVATAR).prev()?.asDomain()
            res = roomName?.content.toModel<RoomAvatarContent>()?.avatarUrl
            if (!res.isNullOrEmpty()) {
                return@doWithRealm
            }
            val roomMembers = RoomMembers(realm, roomId)
            val members = roomMembers.getLoaded()
            if (roomEntity?.membership == MyMembership.INVITED) {
                if (members.size == 1) {
                    res = members.entries.first().value.avatarUrl
                } else if (members.size > 1) {
                    val firstOtherMember = members.filterKeys { it != credentials.userId }.values.firstOrNull()
                    res = firstOtherMember?.avatarUrl
                }
            } else {
                // detect if it is a room with no more than 2 members (i.e. an alone or a 1:1 chat)
                if (roomMembers.getNumberOfJoinedMembers() == 1 && members.isNotEmpty()) {
                    res = members.entries.first().value.avatarUrl
                } else if (roomMembers.getNumberOfMembers() == 2 && members.size > 1) {
                    val firstOtherMember = members.filterKeys { it != credentials.userId }.values.firstOrNull()
                    res = firstOtherMember?.avatarUrl
                }
            }

        }
        return res
    }
}
