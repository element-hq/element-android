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

package im.vector.matrix.android.internal.database.mapper

import com.squareup.moshi.JsonDataException
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.di.MoshiProvider
import timber.log.Timber

internal object EventMapper {

    fun map(event: Event, roomId: String): EventEntity {
        val uds = if (event.unsignedData == null) null
        else MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(event.unsignedData)
        val eventEntity = EventEntity()
        // TODO change this as we shouldn't use event everywhere
        eventEntity.eventId = event.eventId
                ?: "$$roomId-${System.currentTimeMillis()}-${event.hashCode()}"
        eventEntity.roomId = event.roomId ?: roomId
        eventEntity.content = ContentMapper.map(event.content)
        val resolvedPrevContent = event.prevContent ?: event.unsignedData?.prevContent
        eventEntity.prevContent = ContentMapper.map(resolvedPrevContent)
        eventEntity.stateKey = event.stateKey
        eventEntity.type = event.type
        eventEntity.sender = event.senderId
        eventEntity.originServerTs = event.originServerTs
        eventEntity.redacts = event.redacts
        eventEntity.age = event.unsignedData?.age ?: event.originServerTs
        eventEntity.unsignedData = uds
        return eventEntity
    }

    fun map(event: Event, roomId: String, sendState: SendState, ageLocalTs: Long?): im.vector.matrix.sqldelight.session.EventEntity {
        val uds = if (event.unsignedData == null) {
            null
        } else {
            MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(event.unsignedData)
        }
        val resolvedPrevContent = event.prevContent ?: event.unsignedData?.prevContent
        return im.vector.matrix.sqldelight.session.EventEntity.Impl(
                // TODO change this as we shouldn't use event everywhere
                event_id = event.eventId
                        ?: "$$roomId-${System.currentTimeMillis()}-${event.hashCode()}",
                room_id = event.roomId ?: roomId,
                content = ContentMapper.map(event.content),
                prev_content = ContentMapper.map(resolvedPrevContent),
                state_key = event.stateKey,
                type = event.type,
                sender_id = event.senderId,
                origin_server_ts = event.originServerTs,
                redacts = event.redacts,
                age = event.unsignedData?.age ?: event.originServerTs,
                unsigned_data = uds,
                age_local_ts = ageLocalTs,
                send_state = sendState.name,
                decryption_error_code = null,
                decryption_result_json = null
        )
    }

    fun map(eventEntity: EventEntity): Event {
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
                content = ContentMapper.map(eventEntity.content),
                prevContent = ContentMapper.map(eventEntity.prevContent),
                originServerTs = eventEntity.originServerTs,
                senderId = eventEntity.sender,
                stateKey = eventEntity.stateKey,
                roomId = eventEntity.roomId,
                unsignedData = ud,
                redacts = eventEntity.redacts
        ).also {
            it.ageLocalTs = eventEntity.ageLocalTs
            it.sendState = eventEntity.sendState
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
        }
    }

    fun map(eventEntity: im.vector.matrix.sqldelight.session.EventEntity): Event {
        return Event(
                type = eventEntity.type,
                eventId = eventEntity.event_id,
                content = ContentMapper.map(eventEntity.content),
                prevContent = ContentMapper.map(eventEntity.prev_content),
                originServerTs = eventEntity.origin_server_ts,
                senderId = eventEntity.sender_id,
                stateKey = eventEntity.state_key,
                roomId = eventEntity.room_id,
                unsignedData = UnsignedDataMapper.mapFromString(eventEntity.unsigned_data),
                redacts = eventEntity.redacts
        ).also {
            it.ageLocalTs = eventEntity.age_local_ts
            it.sendState = SendState.valueOf(eventEntity.send_state)
            it.setDecryptionValues(eventEntity.decryption_result_json, eventEntity.decryption_error_code)
        }
    }
}

internal fun Event.setDecryptionValues(decryptionResultJson: String?, decryptionErrorCode: String?): Event {
    return apply {
        decryptionResultJson?.let { json ->
            try {
                mxDecryptionResult = MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).fromJson(json)
            } catch (t: JsonDataException) {
                Timber.e(t, "Failed to parse decryption result")
            }
        }
        // TODO get the full crypto error object
        mCryptoError = decryptionErrorCode?.let { errorCode ->
            MXCryptoError.ErrorType.valueOf(errorCode)
        }
    }
}

internal fun EventEntity.asDomain(): Event {
    return EventMapper.map(this)
}

internal fun im.vector.matrix.sqldelight.session.EventEntity.asDomain(): Event {
    return EventMapper.map(this)
}

internal fun Event.toSQLEntity(roomId: String, sendState: SendState, ageLocalTs: Long? = null): im.vector.matrix.sqldelight.session.EventEntity {
    return EventMapper.map(this, roomId, sendState, ageLocalTs)
}


internal fun Event.toEntity(roomId: String, sendState: SendState, ageLocalTs: Long? = null): EventEntity {
    return EventMapper.map(this, roomId).apply {
        this.sendState = sendState
        this.ageLocalTs = ageLocalTs
    }
}
