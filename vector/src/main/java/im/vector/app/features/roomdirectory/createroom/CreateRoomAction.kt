/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.createroom

import android.net.Uri
import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules

sealed class CreateRoomAction : VectorViewModelAction {
    data class SetAvatar(val imageUri: Uri?) : CreateRoomAction()
    data class SetName(val name: String) : CreateRoomAction()
    data class SetTopic(val topic: String) : CreateRoomAction()
    data class SetVisibility(val rule: RoomJoinRules) : CreateRoomAction()
    data class SetRoomAliasLocalPart(val aliasLocalPart: String) : CreateRoomAction()
    data class SetIsEncrypted(val isEncrypted: Boolean) : CreateRoomAction()

    object ToggleShowAdvanced : CreateRoomAction()
    data class DisableFederation(val disableFederation: Boolean) : CreateRoomAction()

    object Create : CreateRoomAction()
    object Reset : CreateRoomAction()
}
