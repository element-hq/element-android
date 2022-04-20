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

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

object TimelineDisplayableEvents {

    /**
     * All types we have an item to build with. Every type not defined here will be shown as DefaultItem if forced to be shown, otherwise will be hidden.
     */
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
            EventType.STATE_ROOM_SERVER_ACL,
            EventType.STATE_ROOM_POWER_LEVELS,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER,
            EventType.CALL_REJECT,
            EventType.ENCRYPTED,
            EventType.STATE_ROOM_ENCRYPTION,
            EventType.STATE_ROOM_GUEST_ACCESS,
            EventType.STATE_ROOM_THIRD_PARTY_INVITE,
            EventType.STICKER,
            EventType.STATE_ROOM_CREATE,
            EventType.STATE_ROOM_TOMBSTONE,
            EventType.STATE_ROOM_JOIN_RULES,
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL,
    ) + EventType.POLL_START + EventType.STATE_ROOM_BEACON_INFO
}

fun TimelineEvent.canBeMerged(): Boolean {
    return root.getClearType() == EventType.STATE_ROOM_MEMBER ||
            root.getClearType() == EventType.STATE_ROOM_SERVER_ACL
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
