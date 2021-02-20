/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.summary

import org.matrix.android.sdk.api.session.events.model.EventType

object RoomSummaryConstants {

    /**
     *
     */
    val PREVIEWABLE_TYPES = listOf(
            // TODO filter message type (KEY_VERIFICATION_READY, etc.)
            EventType.MESSAGE,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_REJECT,
            EventType.CALL_ANSWER,
            EventType.ENCRYPTED,
            EventType.STICKER,
            EventType.REACTION
    )

    // SC addition | this is the Element behaviour previous to Element v1.0.7
    val PREVIEWABLE_TYPES_ALL = listOf(
                // TODO filter message type (KEY_VERIFICATION_READY, etc.)
                EventType.MESSAGE,
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_AVATAR,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER,
                EventType.ENCRYPTED,
                EventType.STATE_ROOM_ENCRYPTION,
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER,
                EventType.REACTION,
                EventType.STATE_ROOM_CREATE
    )

    // SC addition | no reactions in here
    val PREVIEWABLE_ORIGINAL_CONTENT_TYPES = listOf(
                // TODO filter message type (KEY_VERIFICATION_READY, etc.)
                EventType.MESSAGE,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER,
                EventType.ENCRYPTED,
                EventType.STICKER
    )
}
