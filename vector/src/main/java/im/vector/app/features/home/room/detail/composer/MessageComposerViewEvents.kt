/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.command.Command
import im.vector.app.features.command.ParsedCommand

sealed class MessageComposerViewEvents : VectorViewEvents {

    data class AnimateSendButtonVisibility(val isVisible: Boolean) : MessageComposerViewEvents()

    data class ShowMessage(val message: String) : MessageComposerViewEvents()

    abstract class SendMessageResult : MessageComposerViewEvents()

    object MessageSent : SendMessageResult()
    data class JoinRoomCommandSuccess(val roomId: String) : SendMessageResult()
    data class SlashCommandError(val command: Command) : SendMessageResult()
    data class SlashCommandUnknown(val command: String) : SendMessageResult()
    data class SlashCommandNotSupportedInThreads(val command: Command) : SendMessageResult()
    object SlashCommandLoading : SendMessageResult()
    data class SlashCommandResultOk(val parsedCommand: ParsedCommand) : SendMessageResult()
    data class SlashCommandResultError(val throwable: Throwable) : SendMessageResult()

    data class SlashCommandConfirmationRequest(val parsedCommand: ParsedCommand) : MessageComposerViewEvents()

    data class OpenRoomMemberProfile(val userId: String) : MessageComposerViewEvents()

    // TODO Remove
    object SlashCommandNotImplemented : SendMessageResult()

    data class ShowRoomUpgradeDialog(val newVersion: String, val isPublic: Boolean) : MessageComposerViewEvents()

    data class VoicePlaybackOrRecordingFailure(val throwable: Throwable) : MessageComposerViewEvents()

    data class InsertUserDisplayName(val userId: String) : MessageComposerViewEvents()
}
