/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class SpaceAddRoomActions : VectorViewModelAction {
    data class UpdateFilter(val filter: String) : SpaceAddRoomActions()
    data class ToggleSelection(val roomSummary: RoomSummary) : SpaceAddRoomActions()
    object Save : SpaceAddRoomActions()
//    object HandleBack : SpaceAddRoomActions()
}
