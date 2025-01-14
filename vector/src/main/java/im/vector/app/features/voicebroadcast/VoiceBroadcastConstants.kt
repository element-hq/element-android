/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast

import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent

object VoiceBroadcastConstants {

    /** Voice Broadcast State Event. */
    const val STATE_ROOM_VOICE_BROADCAST_INFO = "io.element.voice_broadcast_info"

    /** Custom key passed to the [MessageAudioContent] with Voice Broadcast information. */
    const val VOICE_BROADCAST_CHUNK_KEY = "io.element.voice_broadcast_chunk"

    /** Default voice broadcast chunk duration, in seconds. */
    const val DEFAULT_CHUNK_LENGTH_IN_SECONDS = 120

    /** Maximum length of the voice broadcast in seconds. */
    const val MAX_VOICE_BROADCAST_LENGTH_IN_SECONDS = 14_400 // 4 hours
}
