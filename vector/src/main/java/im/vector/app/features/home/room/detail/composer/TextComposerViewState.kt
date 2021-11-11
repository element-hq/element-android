/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.home.room.detail.RoomDetailArgs
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * Describes the current send mode:
 * REGULAR: sends the text as a regular message
 * QUOTE: User is currently quoting a message
 * EDIT: User is currently editing an existing message
 *
 * Depending on the state the bottom toolbar will change (icons/preview/actions...)
 */
sealed class SendMode(open val text: String) {
    data class REGULAR(
            override val text: String,
            val fromSharing: Boolean,
            // This is necessary for forcing refresh on selectSubscribe
            private val ts: Long = System.currentTimeMillis()
    ) : SendMode(text)

    data class QUOTE(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
    data class EDIT(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
    data class REPLY(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
}

data class TextComposerViewState(
        val roomId: String,
        val canSendMessage: Boolean = true,
        val isSendButtonVisible: Boolean = false,
        val sendMode: SendMode = SendMode.REGULAR("", false),
        val voiceRecordingUiState: VoiceMessageRecorderView.RecordingUiState = VoiceMessageRecorderView.RecordingUiState.None
) : MavericksState {

    val isVoiceRecording = when (voiceRecordingUiState) {
        VoiceMessageRecorderView.RecordingUiState.None,
        VoiceMessageRecorderView.RecordingUiState.Cancelled,
        VoiceMessageRecorderView.RecordingUiState.Playback -> false
        is VoiceMessageRecorderView.DraggingState.Cancelling,
        is VoiceMessageRecorderView.DraggingState.Locking,
        VoiceMessageRecorderView.RecordingUiState.Locked,
        VoiceMessageRecorderView.RecordingUiState.Started  -> true
    }

    val isComposerVisible = canSendMessage && !isVoiceRecording
    val isVoiceMessageRecorderVisible = canSendMessage && !isSendButtonVisible

    constructor(args: RoomDetailArgs) : this(roomId = args.roomId)
}
