/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import im.vector.app.core.platform.VectorViewModelAction

sealed class RoomPermissionsAction : VectorViewModelAction {
    object ToggleShowAllPermissions : RoomPermissionsAction()

    data class UpdatePermission(val editablePermission: EditablePermission, val powerLevel: Int) : RoomPermissionsAction()
}
