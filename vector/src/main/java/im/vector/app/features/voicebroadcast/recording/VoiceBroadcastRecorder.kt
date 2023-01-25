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

package im.vector.app.features.voicebroadcast.recording

import androidx.annotation.IntRange
import im.vector.app.features.voice.VoiceRecorder
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import java.io.File

interface VoiceBroadcastRecorder : VoiceRecorder {

    /** The current chunk number. */
    val currentSequence: Int

    /** Current state of the recorder. */
    val recordingState: State

    /** Current remaining time of recording, in seconds, if any. */
    val currentRemainingTime: Long?

    fun startRecordVoiceBroadcast(voiceBroadcast: VoiceBroadcast, chunkLength: Int, maxLength: Int)

    fun pauseOnError()
    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onVoiceMessageCreated(file: File, @IntRange(from = 1) sequence: Int) = Unit
        fun onStateUpdated(state: State) = Unit
        fun onRemainingTimeUpdated(remainingTime: Long?) = Unit
    }

    enum class State {
        Recording,
        Paused,
        Idle,
        Error,
    }
}
