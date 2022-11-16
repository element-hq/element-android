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

package im.vector.app.features.voicebroadcast

import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastChunk
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent

fun MessageAudioEvent?.isVoiceBroadcast() = this?.root?.getClearContent()?.get(VoiceBroadcastConstants.VOICE_BROADCAST_CHUNK_KEY) != null

fun MessageAudioEvent.getVoiceBroadcastEventId(): String? = if (isVoiceBroadcast()) root.getRelationContent()?.eventId else null

fun MessageAudioEvent.getVoiceBroadcastChunk(): VoiceBroadcastChunk? {
    @Suppress("UNCHECKED_CAST")
    return (root.getClearContent()?.get(VoiceBroadcastConstants.VOICE_BROADCAST_CHUNK_KEY) as? Content).toModel<VoiceBroadcastChunk>()
}

val MessageAudioEvent.sequence: Int? get() = getVoiceBroadcastChunk()?.sequence

val MessageAudioEvent.duration get() = content.audioInfo?.duration ?: content.audioWaveformInfo?.duration ?: 0

val VoiceBroadcastEvent.voiceBroadcastId
    get() = reference?.eventId

val VoiceBroadcastEvent.isLive
    get() = content?.isLive.orFalse()

val MessageVoiceBroadcastInfoContent.isLive
    get() = voiceBroadcastState != null && voiceBroadcastState != VoiceBroadcastState.STOPPED
