/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import kotlin.random.Random

/**
 * Describes the current send mode:
 * REGULAR: sends the text as a regular message;
 * QUOTE: User is currently quoting a message;
 * EDIT: User is currently editing an existing message.
 *
 * Depending on the state the bottom toolbar will change (icons/preview/actions...).
 */
sealed interface SendMode {
    data class Regular(
            val text: CharSequence,
            val fromSharing: Boolean,
            // This is necessary for forcing refresh on selectSubscribe
            private val random: Int = Random.nextInt()
    ) : SendMode

    data class Quote(val timelineEvent: TimelineEvent, val text: CharSequence) : SendMode
    data class Edit(val timelineEvent: TimelineEvent, val text: CharSequence) : SendMode
    data class Reply(val timelineEvent: TimelineEvent, val text: CharSequence) : SendMode
    data class Voice(val text: String) : SendMode
}

sealed interface CanSendStatus {
    object Allowed : CanSendStatus
    object NoPermission : CanSendStatus
    data class UnSupportedE2eAlgorithm(val algorithm: String?) : CanSendStatus
}

fun CanSendStatus.boolean(): Boolean {
    return when (this) {
        CanSendStatus.Allowed -> true
        CanSendStatus.NoPermission -> false
        is CanSendStatus.UnSupportedE2eAlgorithm -> false
    }
}

data class MessageComposerViewState(
        val roomId: String,
        val isRoomError: Boolean = false,
        val canSendMessage: CanSendStatus = CanSendStatus.Allowed,
        val isSendButtonVisible: Boolean = false,
        val rootThreadEventId: String? = null,
        val startsThread: Boolean = false,
        val sendMode: SendMode = SendMode.Regular("", false),
        val voiceRecordingUiState: VoiceMessageRecorderView.RecordingUiState = VoiceMessageRecorderView.RecordingUiState.Idle,
        val voiceBroadcastState: VoiceBroadcastState? = null,
        val text: CharSequence? = null,
        val isFullScreen: Boolean = false,
) : MavericksState {

    val isVoiceRecording = when (voiceRecordingUiState) {
        VoiceMessageRecorderView.RecordingUiState.Idle -> false
        is VoiceMessageRecorderView.RecordingUiState.Locked,
        VoiceMessageRecorderView.RecordingUiState.Draft,
        is VoiceMessageRecorderView.RecordingUiState.Recording -> true
    }

    val isRecordingVoiceBroadcast = when (voiceBroadcastState) {
        VoiceBroadcastState.STARTED,
        VoiceBroadcastState.RESUMED -> true
        else -> false
    }

    val isVoiceMessageIdle = !isVoiceRecording

    val isComposerVisible = canSendMessage.boolean() && !isVoiceRecording && !isRoomError
    val isVoiceMessageRecorderVisible = canSendMessage.boolean() && !isSendButtonVisible && !isRoomError

    constructor(args: TimelineArgs) : this(
            roomId = args.roomId,
            startsThread = args.threadTimelineArgs?.startsThread.orFalse(),
            rootThreadEventId = args.threadTimelineArgs?.rootThreadEventId
    )

    fun isInThreadTimeline(): Boolean = rootThreadEventId != null
}
