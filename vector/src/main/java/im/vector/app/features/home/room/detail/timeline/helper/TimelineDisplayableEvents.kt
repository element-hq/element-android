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

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.extensions.localDateTime
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

object TimelineDisplayableEvents {

    val DISPLAYABLE_TYPES = listOf(
            EventType.MESSAGE,
            EventType.STATE_ROOM_WIDGET_LEGACY,
            EventType.STATE_ROOM_WIDGET,
            EventType.STATE_ROOM_NAME,
            EventType.STATE_ROOM_TOPIC,
            EventType.STATE_ROOM_AVATAR,
            EventType.STATE_ROOM_MEMBER,
            EventType.STATE_ROOM_ALIASES,
            EventType.STATE_ROOM_CANONICAL_ALIAS,
            EventType.STATE_ROOM_HISTORY_VISIBILITY,
            EventType.STATE_ROOM_POWER_LEVELS,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER,
            EventType.ENCRYPTED,
            EventType.STATE_ROOM_ENCRYPTION,
            EventType.STATE_ROOM_GUEST_ACCESS,
            EventType.STATE_ROOM_THIRD_PARTY_INVITE,
            EventType.STICKER,
            EventType.STATE_ROOM_CREATE,
            EventType.STATE_ROOM_TOMBSTONE,
            EventType.STATE_ROOM_JOIN_RULES,
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL
    )
}

fun TimelineEvent.canBeMerged(): Boolean {
    return root.getClearType() == EventType.STATE_ROOM_MEMBER
}

fun TimelineEvent.isRoomConfiguration(roomCreatorUserId: String?): Boolean {
    return root.isStateEvent() && when (root.getClearType()) {
        EventType.STATE_ROOM_GUEST_ACCESS,
        EventType.STATE_ROOM_HISTORY_VISIBILITY,
        EventType.STATE_ROOM_JOIN_RULES,
        EventType.STATE_ROOM_NAME,
        EventType.STATE_ROOM_TOPIC,
        EventType.STATE_ROOM_AVATAR,
        EventType.STATE_ROOM_ALIASES,
        EventType.STATE_ROOM_CANONICAL_ALIAS,
        EventType.STATE_ROOM_POWER_LEVELS,
        EventType.STATE_ROOM_ENCRYPTION -> true
        EventType.STATE_ROOM_MEMBER     -> {
            // Keep only room member events regarding the room creator (when he joined the room),
            // but exclude events where the room creator invite others, or where others join
            roomCreatorUserId != null && root.stateKey == roomCreatorUserId
        }
        else                            -> false
    }
}

fun List<TimelineEvent>.nextSameTypeEvents(index: Int, minSize: Int): List<TimelineEvent> {
    if (index >= size - 1) {
        return emptyList()
    }
    val timelineEvent = this[index]
    val nextSubList = subList(index + 1, size)
    val indexOfNextDay = nextSubList.indexOfFirst {
        val date = it.root.localDateTime()
        val nextDate = timelineEvent.root.localDateTime()
        date.toLocalDate() != nextDate.toLocalDate()
    }
    val nextSameDayEvents = if (indexOfNextDay == -1) {
        nextSubList
    } else {
        nextSubList.subList(0, indexOfNextDay)
    }
    val indexOfFirstDifferentEventType = nextSameDayEvents.indexOfFirst { it.root.getClearType() != timelineEvent.root.getClearType() }
    val sameTypeEvents = if (indexOfFirstDifferentEventType == -1) {
        nextSameDayEvents
    } else {
        nextSameDayEvents.subList(0, indexOfFirstDifferentEventType)
    }
    if (sameTypeEvents.size < minSize) {
        return emptyList()
    }
    return sameTypeEvents
}

fun List<TimelineEvent>.prevSameTypeEvents(index: Int, minSize: Int): List<TimelineEvent> {
    val prevSub = subList(0, index + 1)
    return prevSub
            .reversed()
            .nextSameTypeEvents(0, minSize)
            .reversed()
}
