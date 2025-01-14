/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom

sealed class RoomDirectoryAction : VectorViewModelAction {
    data class SetRoomDirectoryData(val roomDirectoryData: RoomDirectoryData) : RoomDirectoryAction()
    data class FilterWith(val filter: String) : RoomDirectoryAction()
    object LoadMore : RoomDirectoryAction()
    data class JoinRoom(val publicRoom: PublicRoom) : RoomDirectoryAction()
}
