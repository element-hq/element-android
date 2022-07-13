/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.timeline.fixtures

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType

internal fun aTextMessageEvent(
        roomId: String = "room-id",
        eventId: String = "event-id",
        senderId: String = "sender-id",
        body: String = "body"
): Event = Event(
        type = EventType.MESSAGE,
        eventId = eventId,
        content = MessageTextContent(
                msgType = MessageType.MSGTYPE_TEXT,
                body = body
        ).toContent(),
        prevContent = null,
        originServerTs = 0,
        senderId = senderId,
        stateKey = null,
        roomId = roomId,
        unsignedData = null,
        redacts = null
)

internal fun aRoomCreateEvent(
        roomId: String = "room-id",
        eventId: String = "event-id",
        senderId: String = "sender-id",
): Event = Event(
        type = EventType.STATE_ROOM_CREATE,
        eventId = eventId,
        content = RoomCreateContent(
                creator = senderId
        ).toContent(),
        prevContent = null,
        originServerTs = 0,
        senderId = senderId,
        stateKey = "",
        roomId = roomId,
        unsignedData = null,
        redacts = null
)

internal fun aRoomMemberEvent(
        roomId: String = "room-id",
        eventId: String = "event-id",
        senderId: String = "sender-id",
        membership: Membership = Membership.JOIN,
        displayName: String = "Alice",

        ): Event = Event(
        type = EventType.STATE_ROOM_MEMBER,
        eventId = eventId,
        content = RoomMemberContent(
                membership = membership,
                displayName = displayName
        ).toContent(),
        prevContent = null,
        originServerTs = 0,
        senderId = senderId,
        stateKey = senderId,
        roomId = roomId,
        unsignedData = null,
        redacts = null
)

internal fun aRoomNameEvent(
        roomId: String = "room-id",
        eventId: String = "event-id",
        senderId: String = "sender-id",
        roomName: String = "Alice and Bob Room"
): Event = Event(
        type = EventType.STATE_ROOM_NAME,
        eventId = eventId,
        content = RoomNameContent(
                name = roomName
        ).toContent(),
        prevContent = null,
        originServerTs = 0,
        senderId = senderId,
        stateKey = "",
        roomId = roomId,
        unsignedData = null,
        redacts = null
)

internal fun aListOfTextMessageEvents(
        size: Int = 10,
        roomId: String = "room-id",
        eventIdPrefix: String = "event-id",
        senderId: String = "sender-id",
        bodyPrefix: String = "body"
) = (0 until size).map {
    aTextMessageEvent(
            roomId = roomId,
            eventId = "{$eventIdPrefix}_{$it}",
            senderId = senderId,
            body = "{$bodyPrefix}_{$it}",
    )
}
