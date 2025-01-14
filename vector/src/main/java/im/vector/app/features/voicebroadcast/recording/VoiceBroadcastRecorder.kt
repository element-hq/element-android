/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
