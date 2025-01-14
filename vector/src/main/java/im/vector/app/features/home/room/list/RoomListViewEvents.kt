/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.room.model.RoomSummary

/**
 * Transient events for RoomList.
 */
sealed class RoomListViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : RoomListViewEvents()
    data class Failure(val throwable: Throwable) : RoomListViewEvents()

    data class SelectRoom(val roomSummary: RoomSummary, val isInviteAlreadyAccepted: Boolean = false) : RoomListViewEvents()
    object Done : RoomListViewEvents()
    data class NavigateToMxToBottomSheet(val link: String) : RoomListViewEvents()
}
