/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.userdirectory.PendingSelection

sealed class InviteUsersToRoomAction : VectorViewModelAction {
    data class InviteSelectedUsers(val selections: Set<PendingSelection>) : InviteUsersToRoomAction()
}
