/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import im.vector.app.core.platform.VectorViewModelAction

sealed class SpaceLeaveViewAction : VectorViewModelAction {
    object SetAutoLeaveAll : SpaceLeaveViewAction()
    object SetAutoLeaveNone : SpaceLeaveViewAction()
    object SetAutoLeaveSelected : SpaceLeaveViewAction()
    object LeaveSpace : SpaceLeaveViewAction()
}
