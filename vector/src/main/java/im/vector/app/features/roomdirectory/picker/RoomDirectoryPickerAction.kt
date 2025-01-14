/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.picker

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.roomdirectory.RoomDirectoryServer

sealed class RoomDirectoryPickerAction : VectorViewModelAction {
    object Retry : RoomDirectoryPickerAction()
    object EnterEditMode : RoomDirectoryPickerAction()
    object ExitEditMode : RoomDirectoryPickerAction()
    data class SetServerUrl(val url: String) : RoomDirectoryPickerAction()
    data class RemoveServer(val roomDirectoryServer: RoomDirectoryServer) : RoomDirectoryPickerAction()

    object Submit : RoomDirectoryPickerAction()
}
