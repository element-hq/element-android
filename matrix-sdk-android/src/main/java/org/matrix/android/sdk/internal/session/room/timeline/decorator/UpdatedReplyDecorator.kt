/*
 * Copyright (c) The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline.decorator

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getRelationContent
import org.matrix.android.sdk.api.session.room.timeline.isReply
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import java.util.concurrent.atomic.AtomicReference

internal class UpdatedReplyDecorator(
        private val realm: AtomicReference<Realm>,
        private val roomId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val timelineEventMapper: TimelineEventMapper,
) : TimelineEventDecorator {

    override fun decorate(timelineEvent: TimelineEvent): TimelineEvent {
        return if (timelineEvent.isReply() && !timelineEvent.root.isThread()) {
            val newRepliedEvent = createNewRepliedEvent(timelineEvent) ?: return timelineEvent
            timelineEvent.copy(root = newRepliedEvent)
        } else {
            timelineEvent
        }
    }

    private fun createNewRepliedEvent(currentTimelineEvent: TimelineEvent): Event? {
        val relatesEventId = currentTimelineEvent.getRelationContent()?.inReplyTo?.eventId ?: return null
        val timelineEventEntity = TimelineEventEntity.where(
                realm.get(),
                roomId,
                relatesEventId
        ).findFirst() ?: return null

        val isRedactedEvent = timelineEventEntity.root?.asDomain()?.isRedacted() ?: false

        val replyText = localEchoEventFactory
                .bodyForReply(currentTimelineEvent.getLastMessageContent(), true).takeFormatted()

        val newContent = localEchoEventFactory.createReplyTextContent(
                timelineEventMapper.map(timelineEventEntity),
                replyText,
                null,
                false,
                showInThread = false,
                isRedactedEvent = isRedactedEvent
        ).toContent()

        val decryptionResultToSet = currentTimelineEvent.root.mxDecryptionResult?.copy(
                payload = mapOf(
                        "content" to newContent,
                        "type" to EventType.MESSAGE
                )
        )

        val contentToSet = if (currentTimelineEvent.isEncrypted()) {
            // Keep encrypted content as is
            currentTimelineEvent.root.content
        } else {
            // Use new content
            newContent
        }

        return currentTimelineEvent.root.copyAll(
                content = contentToSet,
                mxDecryptionResult = decryptionResultToSet,
                mCryptoError = null,
                mCryptoErrorReason = null
        )
    }
}
