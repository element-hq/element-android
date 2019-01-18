/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.room.members

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.next
import im.vector.matrix.android.internal.database.query.where
import io.realm.RealmQuery

internal class RoomMemberExtractor(private val monarchy: Monarchy,
                                   private val roomId: String) {

    private val cached = HashMap<String, RoomMember?>()

    fun extractFrom(event: EventEntity): RoomMember? {
        if (cached.containsKey(event.eventId)) {
            return cached[event.eventId]
        }
        val sender = event.sender ?: return null
        // If the event is unlinked we want to fetch unlinked state events
        val unlinked = event.isUnlinked
        // When stateIndex is negative, we try to get the next stateEvent prevContent()
        // If prevContent is null we fallback to the Int.MIN state events content()
        val content = if (event.stateIndex <= 0) {
            baseQuery(monarchy, roomId, sender, unlinked).next(from = event.stateIndex)?.prevContent
                    ?: baseQuery(monarchy, roomId, sender, unlinked).last(since = event.stateIndex)?.content
        } else {
            baseQuery(monarchy, roomId, sender, unlinked).last(since = event.stateIndex)?.content
        }
        val roomMember: RoomMember? = ContentMapper.map(content).toModel()
        cached[event.eventId] = roomMember
        return roomMember
    }

    private fun baseQuery(monarchy: Monarchy,
                          roomId: String,
                          sender: String,
                          isUnlinked: Boolean): RealmQuery<EventEntity> {

        lateinit var query: RealmQuery<EventEntity>
        val filterMode = if (isUnlinked) EventEntity.LinkFilterMode.UNLINKED_ONLY else EventEntity.LinkFilterMode.LINKED_ONLY
        monarchy.doWithRealm { realm ->
            query = EventEntity
                    .where(realm, roomId = roomId, type = EventType.STATE_ROOM_MEMBER, linkFilterMode = filterMode)
                    .equalTo(EventEntityFields.STATE_KEY, sender)
        }
        return query
    }


}