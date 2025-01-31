/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.session.room.timeline

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import kotlin.random.Random

object RoomDataHelper {

    private const val FAKE_TEST_SENDER = "@sender:test.org"
    private val EVENT_FACTORIES = hashMapOf(
            0 to { createFakeMessageEvent() },
            1 to { createFakeRoomMemberEvent() }
    )

    fun createFakeListOfEvents(size: Int = 10): List<Event> {
        return (0 until size).mapNotNull {
            val nextInt = Random.nextInt(EVENT_FACTORIES.size)
            EVENT_FACTORIES[nextInt]?.invoke()
        }
    }

    private fun createFakeEvent(
            type: String,
            content: Content? = null,
            prevContent: Content? = null,
            sender: String = FAKE_TEST_SENDER,
            stateKey: String? = null
    ): Event {
        return Event(
                type = type,
                eventId = Random.nextLong().toString(),
                content = content,
                prevContent = prevContent,
                senderId = sender,
                stateKey = stateKey
        )
    }

    fun createFakeMessageEvent(): Event {
        val message = MessageTextContent(MessageType.MSGTYPE_TEXT, "Fake message #${Random.nextLong()}").toContent()
        return createFakeEvent(EventType.MESSAGE, message)
    }

    private fun createFakeRoomMemberEvent(): Event {
        val roomMember = RoomMemberContent(Membership.JOIN, "Fake name #${Random.nextLong()}").toContent()
        return createFakeEvent(EventType.STATE_ROOM_MEMBER, roomMember, stateKey = FAKE_TEST_SENDER)
    }
}
