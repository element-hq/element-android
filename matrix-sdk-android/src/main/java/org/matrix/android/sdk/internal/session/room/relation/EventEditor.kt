/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.relation

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import timber.log.Timber
import javax.inject.Inject

internal class EventEditor @Inject constructor(private val eventSenderProcessor: EventSenderProcessor,
                                               private val eventFactory: LocalEchoEventFactory,
                                               private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
                                               private val localEchoRepository: LocalEchoRepository) {

    fun editTextMessage(targetEvent: TimelineEvent,
                        msgType: String,
                        newBodyText: CharSequence,
                        newBodyAutoMarkdown: Boolean,
                        compatibilityBodyText: String): Cancelable {
        val roomId = targetEvent.roomId
        if (targetEvent.root.sendState.hasFailed()) {
            // We create a new in memory event for the EventSenderProcessor but we keep the eventId of the failed event.
            val editedEvent = eventFactory.createTextEvent(roomId, msgType, newBodyText, newBodyAutoMarkdown).copy(
                    eventId = targetEvent.eventId
            )
            updateFailedEchoWithEvent(roomId, targetEvent.eventId, editedEvent)
            return eventSenderProcessor.postEvent(editedEvent, cryptoSessionInfoProvider.isRoomEncrypted(roomId))
        } else if (targetEvent.root.sendState.isSent()) {
            val event = eventFactory
                    .createReplaceTextEvent(roomId, targetEvent.eventId, newBodyText, newBodyAutoMarkdown, msgType, compatibilityBodyText)
                    .also { localEchoRepository.createLocalEcho(it) }
            return eventSenderProcessor.postEvent(event, cryptoSessionInfoProvider.isRoomEncrypted(roomId))
        } else {
            // Should we throw?
            Timber.w("Can't edit a sending event")
            return NoOpCancellable
        }
    }

    fun editReply(replyToEdit: TimelineEvent,
                  originalTimelineEvent: TimelineEvent,
                  newBodyText: String,
                  compatibilityBodyText: String): Cancelable {
        val roomId = replyToEdit.roomId
        if (replyToEdit.root.sendState.hasFailed()) {
            // We create a new in memory event for the EventSenderProcessor but we keep the eventId of the failed event.
            val editedEvent = eventFactory.createReplyTextEvent(roomId, originalTimelineEvent, newBodyText, false)?.copy(
                    eventId = replyToEdit.eventId
            ) ?: return NoOpCancellable
            updateFailedEchoWithEvent(roomId, replyToEdit.eventId, editedEvent)
            return eventSenderProcessor.postEvent(editedEvent, cryptoSessionInfoProvider.isRoomEncrypted(roomId))
        } else if (replyToEdit.root.sendState.isSent()) {
            val event = eventFactory.createReplaceTextOfReply(
                    roomId,
                    replyToEdit,
                    originalTimelineEvent,
                    newBodyText,
                    true,
                    MessageType.MSGTYPE_TEXT,
                    compatibilityBodyText
            )
                    .also { localEchoRepository.createLocalEcho(it) }
            return eventSenderProcessor.postEvent(event, cryptoSessionInfoProvider.isRoomEncrypted(roomId))
        } else {
            // Should we throw?
            Timber.w("Can't edit a sending event")
            return NoOpCancellable
        }
    }

    private fun updateFailedEchoWithEvent(roomId: String, failedEchoEventId: String, editedEvent: Event) {
        val editedEventEntity = editedEvent.toEntity(roomId, SendState.UNSENT, System.currentTimeMillis())
        localEchoRepository.updateEchoAsync(failedEchoEventId) { _, entity ->
            entity.content = editedEventEntity.content
            entity.ageLocalTs = editedEventEntity.ageLocalTs
            entity.age = editedEventEntity.age
            entity.originServerTs = editedEventEntity.originServerTs
            entity.sendState = editedEventEntity.sendState
            entity.sendStateDetails = editedEventEntity.sendStateDetails
        }
    }
}
