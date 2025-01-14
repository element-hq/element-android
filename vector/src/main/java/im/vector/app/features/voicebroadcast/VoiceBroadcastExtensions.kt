/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
