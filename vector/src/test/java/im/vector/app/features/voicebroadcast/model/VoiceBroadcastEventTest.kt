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

package im.vector.app.features.voicebroadcast.model

import im.vector.app.features.voicebroadcast.STATE_ROOM_VOICE_BROADCAST_INFO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.AudioInfo
import org.matrix.android.sdk.api.session.room.model.message.AudioWaveformInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.model.relation.ReplyToContent

private const val AN_EVENT_ID = "event_id"
private const val A_REFERENCED_EVENT_ID = "event_id_ref"
private const val A_CHUNK_LENGTH = 3_600L

class VoiceBroadcastEventTest {

    @Test
    fun `given a started Voice Broadcast Event, when mapping to VoiceBroadcastEvent, then return expected object`() {
        // Given
        val content = MessageVoiceBroadcastInfoContent(
                voiceBroadcastStateStr = VoiceBroadcastState.STARTED.value,
                chunkLength = A_CHUNK_LENGTH,
                relatesTo = RelationDefaultContent(RelationType.REFERENCE, A_REFERENCED_EVENT_ID),
        )
        val event = Event(
                eventId = AN_EVENT_ID,
                type = STATE_ROOM_VOICE_BROADCAST_INFO,
                content = content.toContent(),
        )
        val expectedReference = RelationDefaultContent(RelationType.REFERENCE, event.eventId)

        // When
        val result = event.asVoiceBroadcastEvent()

        // Then
        result.shouldNotBeNull()
        result.content shouldBeEqualTo content
        result.reference shouldBeEqualTo expectedReference
    }

    @Test
    fun `given a not started Voice Broadcast Event, when mapping to VoiceBroadcastEvent, then return expected object`() {
        // Given
        val content = MessageVoiceBroadcastInfoContent(
                voiceBroadcastStateStr = VoiceBroadcastState.PAUSED.value,
                chunkLength = A_CHUNK_LENGTH,
                relatesTo = RelationDefaultContent(RelationType.REFERENCE, A_REFERENCED_EVENT_ID),
        )
        val event = Event(
                type = STATE_ROOM_VOICE_BROADCAST_INFO,
                content = content.toContent(),
        )
        val expectedReference = content.relatesTo

        // When
        val result = event.asVoiceBroadcastEvent()

        // Then
        result.shouldNotBeNull()
        result.content shouldBeEqualTo content
        result.reference shouldBeEqualTo expectedReference
    }

    @Test
    fun `given a non Voice Broadcast Event, when mapping to VoiceBroadcastEvent, then return null`() {
        // Given
        val content = MessageAudioContent(
                msgType = MessageType.MSGTYPE_AUDIO,
                body = "audio",
                audioInfo = AudioInfo(
                        duration = 300,
                        mimeType = "",
                        size = 500L
                ),
                url = "a_url",
                audioWaveformInfo = AudioWaveformInfo(
                        duration = 300,
                        waveform = null
                ),
                voiceMessageIndicator = emptyMap(),
                relatesTo = RelationDefaultContent(
                        type = RelationType.THREAD,
                        eventId = AN_EVENT_ID,
                        isFallingBack = true,
                        inReplyTo = ReplyToContent(eventId = A_REFERENCED_EVENT_ID)
                )
        )
        val event = Event(
                type = EventType.MESSAGE,
                content = content.toContent(),
        )

        // When
        val result = event.asVoiceBroadcastEvent()

        // Then
        result.shouldBeNull()
    }
}
