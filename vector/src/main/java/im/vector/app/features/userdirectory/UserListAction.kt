/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.userdirectory

import im.vector.app.core.platform.VectorViewModelAction

sealed class UserListAction : VectorViewModelAction {
    data class SearchUsers(val value: String) : UserListAction()
    object ClearSearchUsers : UserListAction()
    data class AddPendingSelection(val pendingSelection: PendingSelection) : UserListAction()
    data class RemovePendingSelection(val pendingSelection: PendingSelection) : UserListAction()
    object ComputeMatrixToLinkForSharing : UserListAction()
    object UserConsentRequest : UserListAction()
    data class UpdateUserConsent(val consent: Boolean) : UserListAction()
    object Resumed : UserListAction()
}
