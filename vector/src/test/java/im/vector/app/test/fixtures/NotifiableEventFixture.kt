/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test.fixtures

import im.vector.app.features.notifications.InviteNotifiableEvent
import im.vector.app.features.notifications.NotifiableMessageEvent
import im.vector.app.features.notifications.SimpleNotifiableEvent

fun aSimpleNotifiableEvent(eventId: String, type: String? = null) = SimpleNotifiableEvent(
        matrixID = null,
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        title = "title",
        description = "description",
        type = type,
        timestamp = 0,
        soundName = null,
        canBeReplaced = false,
        isRedacted = false
)

fun anInviteNotifiableEvent(roomId: String) = InviteNotifiableEvent(
        matrixID = null,
        eventId = "event-id",
        roomId = roomId,
        roomName = "a room name",
        editedEventId = null,
        noisy = false,
        title = "title",
        description = "description",
        type = null,
        timestamp = 0,
        soundName = null,
        canBeReplaced = false,
        isRedacted = false
)

fun aNotifiableMessageEvent(eventId: String, roomId: String) = NotifiableMessageEvent(
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        timestamp = 0,
        senderName = "sender-name",
        senderId = "sending-id",
        body = "message-body",
        roomId = roomId,
        roomName = "room-name",
        roomIsDirect = false,
        canBeReplaced = false,
        isRedacted = false
)
