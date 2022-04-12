/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import com.squareup.moshi.JsonDataException
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.threads.ThreadDetails
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal object EventMapper {

    fun map(event: Event, roomId: String): EventEntity {
        val eventEntity = EventEntity()
        // TODO change this as we shouldn't use event everywhere
        eventEntity.eventId = event.eventId ?: "$$roomId-${System.currentTimeMillis()}-${event.hashCode()}"
        eventEntity.roomId = event.roomId ?: roomId
        eventEntity.content = ContentMapper.map(event.content)
        eventEntity.prevContent = ContentMapper.map(event.resolvedPrevContent())
        eventEntity.isUseless = IsUselessResolver.isUseless(event)
        eventEntity.stateKey = event.stateKey
        eventEntity.type = event.type ?: EventType.MISSING_TYPE
        eventEntity.sender = event.senderId
        eventEntity.originServerTs = event.originServerTs
        eventEntity.redacts = event.redacts
        eventEntity.age = event.unsignedData?.age ?: event.originServerTs
        eventEntity.unsignedData = event.unsignedData?.let {
            MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(it)
        }
        eventEntity.decryptionResultJson = event.mxDecryptionResult?.let {
            MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).toJson(it)
        }
        eventEntity.decryptionErrorReason = event.mCryptoErrorReason
        eventEntity.decryptionErrorCode = event.mCryptoError?.name
        eventEntity.isRootThread = event.threadDetails?.isRootThread ?: false
        eventEntity.rootThreadEventId = event.getRootThreadEventId()
        eventEntity.numberOfThreads = event.threadDetails?.numberOfThreads ?: 0
        eventEntity.threadNotificationState = event.threadDetails?.threadNotificationState ?: ThreadNotificationState.NO_NEW_MESSAGE
        return eventEntity
    }

    fun map(eventEntity: EventEntity, castJsonNumbers: Boolean = false): Event {
        val ud = eventEntity.unsignedData
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    try {
                        MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).fromJson(it)
                    } catch (t: JsonDataException) {
                        Timber.e(t, "Failed to parse UnsignedData")
                        null
                    }
                }

        return Event(
                type = eventEntity.type,
                eventId = eventEntity.eventId,
                content = ContentMapper.map(eventEntity.content, castJsonNumbers),
                prevContent = ContentMapper.map(eventEntity.prevContent, castJsonNumbers),
                originServerTs = eventEntity.originServerTs,
                senderId = eventEntity.sender,
                stateKey = eventEntity.stateKey,
                roomId = eventEntity.roomId,
                unsignedData = ud,
                redacts = eventEntity.redacts
        ).also {
            it.ageLocalTs = eventEntity.ageLocalTs
            it.sendState = eventEntity.sendState
            it.sendStateDetails = eventEntity.sendStateDetails
            eventEntity.decryptionResultJson?.let { json ->
                try {
                    it.mxDecryptionResult = MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).fromJson(json)
                } catch (t: JsonDataException) {
                    Timber.e(t, "Failed to parse decryption result")
                }
            }
            // TODO get the full crypto error object
            it.mCryptoError = eventEntity.decryptionErrorCode?.let { errorCode ->
                MXCryptoError.ErrorType.valueOf(errorCode)
            }
            it.mCryptoErrorReason = eventEntity.decryptionErrorReason
            it.threadDetails = ThreadDetails(
                    isRootThread = eventEntity.isRootThread,
                    isThread = if (it.threadDetails?.isThread == true) true else eventEntity.isThread(),
                    numberOfThreads = eventEntity.numberOfThreads,
                    threadSummarySenderInfo = eventEntity.threadSummaryLatestMessage?.let { timelineEventEntity ->
                        SenderInfo(
                                userId = timelineEventEntity.root?.sender ?: "",
                                displayName = timelineEventEntity.senderName,
                                isUniqueDisplayName = timelineEventEntity.isUniqueDisplayName,
                                avatarUrl = timelineEventEntity.senderAvatar
                        )
                    },
                    threadNotificationState = eventEntity.threadNotificationState,
                    threadSummaryLatestEvent = eventEntity.threadSummaryLatestMessage?.root?.asDomain(),
                    lastMessageTimestamp = eventEntity.threadSummaryLatestMessage?.root?.originServerTs

            )
        }
    }
}

internal fun EventEntity.asDomain(castJsonNumbers: Boolean = false): Event {
    return EventMapper.map(this, castJsonNumbers)
}

internal fun Event.toEntity(roomId: String, sendState: SendState, ageLocalTs: Long?, contentToInject: String? = null): EventEntity {
    return EventMapper.map(this, roomId).apply {
        this.sendState = sendState
        this.ageLocalTs = ageLocalTs
        contentToInject?.let {
            this.content = it
            if (this.type == EventType.STICKER) {
                this.type = EventType.MESSAGE
            }
        }
    }
}
