/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.createdirect

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.userdirectory.PendingSelection

sealed class CreateDirectRoomAction : VectorViewModelAction {
    data class PrepareRoomWithSelectedUsers(
            val selections: Set<PendingSelection>
    ) : CreateDirectRoomAction()

    object CreateRoomAndInviteSelectedUsers : CreateDirectRoomAction()

    data class QrScannedAction(
            val result: String
    ) : CreateDirectRoomAction()
}
