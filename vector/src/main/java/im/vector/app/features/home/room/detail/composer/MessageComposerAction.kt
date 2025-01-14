/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.command.ParsedCommand
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent

sealed class MessageComposerAction : VectorViewModelAction {
    data class SendMessage(val text: CharSequence, val formattedText: String?, val autoMarkdown: Boolean) : MessageComposerAction()
    data class EnterEditMode(val eventId: String) : MessageComposerAction()
    data class EnterQuoteMode(val eventId: String) : MessageComposerAction()
    data class EnterReplyMode(val eventId: String) : MessageComposerAction()
    data class EnterRegularMode(val fromSharing: Boolean) : MessageComposerAction()
    data class UserIsTyping(val isTyping: Boolean) : MessageComposerAction()
    data class OnTextChanged(val text: CharSequence) : MessageComposerAction()
    data class OnEntersBackground(val composerText: String) : MessageComposerAction()
    data class SlashCommandConfirmed(val parsedCommand: ParsedCommand) : MessageComposerAction()
    data class InsertUserDisplayName(val userId: String) : MessageComposerAction()
    data class SetFullScreen(val isFullScreen: Boolean) : MessageComposerAction()

    // Voice Message
    data class InitializeVoiceRecorder(val attachmentData: ContentAttachmentData) : MessageComposerAction()
    data class OnVoiceRecordingUiStateChanged(val uiState: VoiceMessageRecorderView.RecordingUiState) : MessageComposerAction()
    object StartRecordingVoiceMessage : MessageComposerAction()
    data class EndRecordingVoiceMessage(val isCancelled: Boolean, val rootThreadEventId: String?) : MessageComposerAction()
    object PauseRecordingVoiceMessage : MessageComposerAction()
    data class PlayOrPauseVoicePlayback(val eventId: String, val messageAudioContent: MessageAudioContent) : MessageComposerAction()
    object PlayOrPauseRecordingPlayback : MessageComposerAction()
    data class VoiceWaveformTouchedUp(val eventId: String, val duration: Int, val percentage: Float) : MessageComposerAction()
    data class VoiceWaveformMovedTo(val eventId: String, val duration: Int, val percentage: Float) : MessageComposerAction()
    data class AudioSeekBarMovedTo(val eventId: String, val duration: Int, val percentage: Float) : MessageComposerAction()
}
