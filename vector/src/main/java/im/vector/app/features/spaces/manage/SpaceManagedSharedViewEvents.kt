/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import im.vector.app.core.platform.VectorViewEvents

sealed class SpaceManagedSharedViewEvents : VectorViewEvents {
    object Finish : SpaceManagedSharedViewEvents()
    object ShowLoading : SpaceManagedSharedViewEvents()
    object HideLoading : SpaceManagedSharedViewEvents()
    object NavigateToCreateRoom : SpaceManagedSharedViewEvents()
    object NavigateToCreateSpace : SpaceManagedSharedViewEvents()
    object NavigateToManageRooms : SpaceManagedSharedViewEvents()
    object NavigateToAliasSettings : SpaceManagedSharedViewEvents()
    object NavigateToPermissionSettings : SpaceManagedSharedViewEvents()
}
