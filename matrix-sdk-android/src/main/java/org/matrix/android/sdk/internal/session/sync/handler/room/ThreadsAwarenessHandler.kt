/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.tag.RoomTagContent
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomTagEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.permalinks.PermalinkFactory
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import timber.log.Timber
import javax.inject.Inject

/**
 * This handler is responsible for a smooth threads migration. It will map all incoming
 * threads as replies. So a device without threads enabled/updated will be able to view
 * threads response as replies to the original message
 */
internal class ThreadsAwarenessHandler @Inject constructor(
        private val permalinkFactory: PermalinkFactory
) {

    fun handleIfNeeded(realm: Realm,
                       roomId: String,
                       event: Event,
                       isInitialSync: Boolean,
                       decryptIfNeeded: (event: Event, roomId: String) -> Unit) {

        if (!isThreadEvent(event)) return
        val rootThreadEventId = getRootThreadEventId(event) ?: return
        val payload = event.mxDecryptionResult?.payload?.toMutableMap() ?: return
        val body = getValueFromPayload(payload, "body") ?: return
        val msgType = getValueFromPayload(payload, "msgtype") ?: return
        val rootThreadEventEntity = EventEntity.where(realm, eventId = rootThreadEventId).findFirst() ?: return
        val rootThreadEvent = EventMapper.map(rootThreadEventEntity)
        val rootThreadEventSenderId = rootThreadEvent.senderId ?: return

        Timber.i("------> Thread event detected! - isInitialSync: $isInitialSync")

        if (rootThreadEvent.isEncrypted()) {
            decryptIfNeeded(rootThreadEvent, roomId)
        }

        val rootThreadEventBody = getValueFromPayload(rootThreadEvent.mxDecryptionResult?.payload?.toMutableMap(),"body")

        val permalink = permalinkFactory.createPermalink(roomId, rootThreadEventId, false)
        val userLink =  permalinkFactory.createPermalink(rootThreadEventSenderId, false) ?: ""

        val replyFormatted = LocalEchoEventFactory.REPLY_PATTERN.format(
                permalink,
                userLink,
                rootThreadEventSenderId,
                // Remove inner mx_reply tags if any
                rootThreadEventBody,
                body)

        val messageTextContent =  MessageTextContent(
                msgType = msgType,
                format = MessageFormat.FORMAT_MATRIX_HTML,
                body = body,
                formattedBody = replyFormatted
        ).toContent()

        payload["content"] = messageTextContent

        event.mxDecryptionResult = event.mxDecryptionResult?.copy(payload = payload )

    }

    private fun isThreadEvent(event: Event): Boolean =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.type == "io.element.thread"

    private fun getRootThreadEventId(event: Event): String? =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.eventId

    @Suppress("UNCHECKED_CAST")
    private fun getValueFromPayload(payload: JsonDict?, key: String): String? {
        val content = payload?.get("content") as? JsonDict
        return content?.get(key) as? String
    }
}
