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

import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.members.RoomMembers
import io.realm.Realm
import io.realm.kotlin.createObject

internal class RoomSummaryUpdater(monarchy: Monarchy,
                                  private val roomDisplayNameResolver: RoomDisplayNameResolver,
                                  private val roomAvatarResolver: RoomAvatarResolver,
                                  private val context: Context,
                                  private val credentials: Credentials
) : RealmLiveEntityObserver<RoomEntity>(monarchy) {

    override val query = Monarchy.Query<RoomEntity> { RoomEntity.where(it) }

    override fun process(inserted: List<RoomEntity>, updated: List<RoomEntity>, deleted: List<RoomEntity>) {
        val rooms = (inserted + updated).map { it.roomId }
        monarchy.writeAsync { realm ->
            rooms.forEach { updateRoom(realm, it) }
        }
    }

    private fun updateRoom(realm: Realm, roomId: String?) {
        if (roomId == null) {
            return
        }
        val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                          ?: realm.createObject(roomId)

        val lastMessageEvent = EventEntity.where(realm, roomId, EventType.MESSAGE).last()
        val lastTopicEvent = EventEntity.where(realm, roomId, EventType.STATE_ROOM_TOPIC).last()?.asDomain()

        val otherRoomMembers = RoomMembers(realm, roomId).getLoaded().filterKeys { it != credentials.userId }

        roomSummary.displayName = roomDisplayNameResolver.resolve(context, roomId).toString()
        roomSummary.avatarUrl = roomAvatarResolver.resolve(roomId)
        roomSummary.topic = lastTopicEvent?.content.toModel<RoomTopicContent>()?.topic
        roomSummary.lastMessage = lastMessageEvent
        roomSummary.otherMemberIds.clear()
        roomSummary.otherMemberIds.addAll(otherRoomMembers.keys)
    }

}