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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType.MSGTYPE_VOICE_BROADCAST_INFO
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

/**
 * Content of the state event of type [VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO].
 *
 * It contains general info related to a voice broadcast.
 */
@JsonClass(generateAdapter = true)
data class MessageVoiceBroadcastInfoContent(
        /** Local message type, not from server. */
        @Transient override val msgType: String = MSGTYPE_VOICE_BROADCAST_INFO,
        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /** The device from which the broadcast has been started. */
        @Json(name = "device_id") val deviceId: String? = null,
        /** The [VoiceBroadcastState] value. **/
        @Json(name = "state") val voiceBroadcastStateStr: String = "",
        /** The length of the voice chunks in seconds. **/
        @Json(name = "chunk_length") val chunkLength: Int? = null,
        /** The sequence of the last sent chunk. **/
        @Json(name = "last_chunk_sequence") val lastChunkSequence: Int? = null,
) : MessageContent {

    val voiceBroadcastState: VoiceBroadcastState? = VoiceBroadcastState.values()
            .find { it.value == voiceBroadcastStateStr }
}
