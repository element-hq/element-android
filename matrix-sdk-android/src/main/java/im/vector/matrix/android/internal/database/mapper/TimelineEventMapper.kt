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
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.database.model.TimelineEventEntity

internal object TimelineEventMapper {

    fun map(timelineEvent: TimelineEvent, roomId: String): TimelineEventEntity {
        val timelineEventEntity = TimelineEventEntity()
        timelineEventEntity.root = timelineEvent.root.toEntity(roomId)
        timelineEventEntity.eventId = timelineEvent.root.eventId ?: ""
        timelineEventEntity.roomId = roomId
        timelineEventEntity.annotations = timelineEvent.annotations?.let { EventAnnotationsSummaryMapper.map(it, roomId) }
        return timelineEventEntity
    }

    fun map(timelineEventEntity: TimelineEventEntity): TimelineEvent {

        return TimelineEvent(
                root = timelineEventEntity.root?.asDomain()
                        ?: Event("", timelineEventEntity.eventId),
                annotations = timelineEventEntity.annotations?.asDomain(),
                localId = timelineEventEntity.localId,
                displayIndex = timelineEventEntity.root?.displayIndex ?: 0,
                senderName = timelineEventEntity.senderName,
                isUniqueDisplayName = timelineEventEntity.isUniqueDisplayName,
                senderAvatar = timelineEventEntity.senderAvatar,
                sendState = timelineEventEntity.root?.sendState ?: SendState.UNKNOWN
        )
    }

}

internal fun TimelineEventEntity.asDomain(): TimelineEvent {
    return TimelineEventMapper.map(this)
}

internal fun TimelineEvent.toEntity(roomId: String): TimelineEventEntity {
    return TimelineEventMapper.map(this, roomId)
}


