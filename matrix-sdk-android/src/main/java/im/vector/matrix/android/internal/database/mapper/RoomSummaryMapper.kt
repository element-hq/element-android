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

import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.session.room.timeline.TimelineEventFactory
import javax.inject.Inject

internal class RoomSummaryMapper @Inject constructor(private val timelineEventFactory: TimelineEventFactory) {

    fun map(roomSummaryEntity: RoomSummaryEntity, getLatestEvent: Boolean = false): RoomSummary {
        val tags = roomSummaryEntity.tags.map {
            RoomTag(it.tagName, it.tagOrder)
        }
        val latestEvent = if (getLatestEvent) {
            roomSummaryEntity.latestEvent?.let {
                timelineEventFactory.create(it, it.realm)
            }
        } else {
            null
        }
        return RoomSummary(
                roomId = roomSummaryEntity.roomId,
                displayName = roomSummaryEntity.displayName ?: "",
                topic = roomSummaryEntity.topic ?: "",
                avatarUrl = roomSummaryEntity.avatarUrl ?: "",
                isDirect = roomSummaryEntity.isDirect,
                latestEvent = latestEvent,
                otherMemberIds = roomSummaryEntity.otherMemberIds.toList(),
                highlightCount = roomSummaryEntity.highlightCount,
                notificationCount = roomSummaryEntity.notificationCount,
                tags = tags,
                membership = roomSummaryEntity.membership
        )
    }
}
