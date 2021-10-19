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

import androidx.annotation.StringRes
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.command.Command

sealed class TextComposerViewEvents : VectorViewEvents {

    data class AnimateSendButtonVisibility(val isVisible: Boolean) : TextComposerViewEvents()

    data class ShowMessage(val message: String) : TextComposerViewEvents()

    abstract class SendMessageResult : TextComposerViewEvents()

    object MessageSent : SendMessageResult()
    data class JoinRoomCommandSuccess(val roomId: String) : SendMessageResult()
    class SlashCommandError(val command: Command) : SendMessageResult()
    class SlashCommandUnknown(val command: String) : SendMessageResult()
    data class SlashCommandHandled(@StringRes val messageRes: Int? = null) : SendMessageResult()
    object SlashCommandResultOk : SendMessageResult()
    class SlashCommandResultError(val throwable: Throwable) : SendMessageResult()

    data class OpenRoomMemberProfile(val userId: String) : TextComposerViewEvents()

    // TODO Remove
    object SlashCommandNotImplemented : SendMessageResult()

    data class ShowRoomUpgradeDialog(val newVersion: String, val isPublic: Boolean) : TextComposerViewEvents()
}
