/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.userdirectory

import im.vector.app.core.platform.VectorSharedAction

sealed class UserListSharedAction : VectorSharedAction {
    object Close : UserListSharedAction()
    object GoBack : UserListSharedAction()
    data class OnMenuItemSubmitClick(val selections: Set<PendingSelection>) : UserListSharedAction()
    object OpenPhoneBook : UserListSharedAction()
    object AddByQrCode : UserListSharedAction()
}
