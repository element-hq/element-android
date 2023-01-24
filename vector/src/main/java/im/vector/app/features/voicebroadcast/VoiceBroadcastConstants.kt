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
