/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import im.vector.app.core.platform.VectorViewEvents

sealed class SpaceManageRoomViewEvents : VectorViewEvents {
    //    object BulkActionSuccess: SpaceManageRoomViewEvents()
    data class BulkActionFailure(val errorList: List<Throwable>) : SpaceManageRoomViewEvents()
}
