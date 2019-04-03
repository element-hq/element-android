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

package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.internal.session.room.media.MediaAttachment

internal class EventFactory(private val credentials: Credentials) {

    fun createTextEvent(roomId: String, text: String): Event {
        val content = MessageTextContent(type = MessageType.MSGTYPE_TEXT, body = text)
        return createEvent(roomId, content)
    }

    fun createImageEvent(roomId: String, attachment: MediaAttachment): Event {
        val content = MessageImageContent(
                type = MessageType.MSGTYPE_IMAGE,
                body = attachment.name ?: "image",
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    fun updateImageEvent(event: Event, url: String): Event {
        val imageContent = event.content.toModel<MessageImageContent>() ?: return event
        val updatedContent = imageContent.copy(url = url)
        return event.copy(content = updatedContent.toContent())
    }

    fun createEvent(roomId: String, content: Any? = null): Event {
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                sender = credentials.userId,
                eventId = dummyEventId(roomId),
                type = EventType.MESSAGE,
                content = content.toContent()
        )
    }

    private fun dummyOriginServerTs(): Long {
        return System.currentTimeMillis()
    }

    private fun dummyEventId(roomId: String): String {
        return roomId + "-" + dummyOriginServerTs()
    }
}
