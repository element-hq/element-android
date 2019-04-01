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

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.internal.database.model.EventEntity


internal object EventMapper {


    fun map(event: Event, roomId: String): EventEntity {
        val eventEntity = EventEntity()
        eventEntity.eventId = event.eventId ?: ""
        eventEntity.roomId = event.roomId ?: roomId
        eventEntity.content = ContentMapper.map(event.content)
        val resolvedPrevContent = event.prevContent ?: event.unsignedData?.prevContent
        eventEntity.prevContent = ContentMapper.map(resolvedPrevContent)
        eventEntity.stateKey = event.stateKey
        eventEntity.type = event.type
        eventEntity.sender = event.sender
        eventEntity.originServerTs = event.originServerTs
        eventEntity.redacts = event.redacts
        eventEntity.age = event.unsignedData?.age ?: event.originServerTs
        return eventEntity
    }

    fun map(eventEntity: EventEntity): Event {
        return Event(
                type = eventEntity.type,
                eventId = eventEntity.eventId,
                content = ContentMapper.map(eventEntity.content),
                prevContent = ContentMapper.map(eventEntity.prevContent),
                originServerTs = eventEntity.originServerTs,
                sender = eventEntity.sender,
                stateKey = eventEntity.stateKey,
                roomId = eventEntity.roomId,
                unsignedData = UnsignedData(eventEntity.age),
                redacts = eventEntity.redacts
        )
    }

}

internal fun EventEntity.asDomain(): Event {
    return EventMapper.map(this)
}

internal fun Event.toEntity(roomId: String): EventEntity {
    return EventMapper.map(this, roomId)
}

