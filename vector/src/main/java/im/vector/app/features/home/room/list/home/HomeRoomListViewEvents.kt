/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class HomeRoomListViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : HomeRoomListViewEvents()
    data class Failure(val throwable: Throwable) : HomeRoomListViewEvents()
    object Done : HomeRoomListViewEvents()
    data class SelectRoom(val roomSummary: RoomSummary, val isInviteAlreadyAccepted: Boolean = false) : HomeRoomListViewEvents()
}
