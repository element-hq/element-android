/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import im.vector.app.core.platform.VectorViewModelAction

sealed class SpaceManagedSharedAction : VectorViewModelAction {
    object HandleBack : SpaceManagedSharedAction()
    object ShowLoading : SpaceManagedSharedAction()
    object HideLoading : SpaceManagedSharedAction()
    object CreateRoom : SpaceManagedSharedAction()
    object CreateSpace : SpaceManagedSharedAction()
    object ManageRooms : SpaceManagedSharedAction()
    object OpenSpaceAliasesSettings : SpaceManagedSharedAction()
    object OpenSpacePermissionSettings : SpaceManagedSharedAction()
}
