/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import im.vector.app.core.platform.VectorViewModelAction

sealed class SpaceManageRoomViewAction : VectorViewModelAction {
    data class ToggleSelection(val roomId: String) : SpaceManageRoomViewAction()
    data class UpdateFilter(val filter: String) : SpaceManageRoomViewAction()
    object ClearSelection : SpaceManageRoomViewAction()
    data class MarkAllAsSuggested(val suggested: Boolean) : SpaceManageRoomViewAction()
    object BulkRemove : SpaceManageRoomViewAction()
    object RefreshFromServer : SpaceManageRoomViewAction()
    object LoadAdditionalItemsIfNeeded : SpaceManageRoomViewAction()
}
