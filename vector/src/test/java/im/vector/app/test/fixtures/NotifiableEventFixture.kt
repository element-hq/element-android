/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.notifications.InviteNotifiableEvent
import im.vector.app.features.notifications.NotifiableMessageEvent
import im.vector.app.features.notifications.SimpleNotifiableEvent

fun aSimpleNotifiableEvent(
        eventId: String = "simple-event-id",
        type: String? = null,
        isRedacted: Boolean = false,
        canBeReplaced: Boolean = false,
        editedEventId: String? = null
) = SimpleNotifiableEvent(
        matrixID = null,
        eventId = eventId,
        editedEventId = editedEventId,
        noisy = false,
        title = "title",
        description = "description",
        type = type,
        timestamp = 0,
        soundName = null,
        canBeReplaced = canBeReplaced,
        isRedacted = isRedacted
)

fun anInviteNotifiableEvent(
        roomId: String = "an-invite-room-id",
        eventId: String = "invite-event-id",
        isRedacted: Boolean = false
) = InviteNotifiableEvent(
        matrixID = null,
        eventId = eventId,
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
        isRedacted = isRedacted
)

fun aNotifiableMessageEvent(
        eventId: String = "a-message-event-id",
        roomId: String = "a-message-room-id",
        threadId: String? = null,
        isRedacted: Boolean = false
) = NotifiableMessageEvent(
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        timestamp = 0,
        senderName = "sender-name",
        senderId = "sending-id",
        body = "message-body",
        roomId = roomId,
        threadId = threadId,
        roomName = "room-name",
        roomIsDirect = false,
        canBeReplaced = false,
        isRedacted = isRedacted,
        imageUriString = null
)
