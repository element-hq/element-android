/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail

sealed class RoomDetailPendingAction {
    object DoNothing : RoomDetailPendingAction()
    data class JumpToReadReceipt(val userId: String) : RoomDetailPendingAction()
    data class MentionUser(val userId: String) : RoomDetailPendingAction()
    data class OpenRoom(val roomId: String, val closeCurrentRoom: Boolean = false) : RoomDetailPendingAction()
}
