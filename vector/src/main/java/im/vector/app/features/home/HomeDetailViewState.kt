/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.util.MatrixItem

data class HomeDetailViewState(
        val selectedSpace: RoomSummary? = null,
        val myMatrixItem: MatrixItem? = null,
        val asyncRooms: Async<List<RoomSummary>> = Uninitialized,
        val currentTab: HomeTab = HomeTab.RoomList(RoomListDisplayMode.PEOPLE),
        val notificationCountCatchup: Int = 0,
        val notificationHighlightCatchup: Boolean = false,
        val notificationCountPeople: Int = 0,
        val notificationHighlightPeople: Boolean = false,
        val notificationCountRooms: Int = 0,
        val notificationHighlightRooms: Boolean = false,
        val hasUnreadMessages: Boolean = false,
        val syncState: SyncState? = null,
        val incrementalSyncRequestState: SyncRequestState.IncrementalSyncRequestState? = null,
        val pushCounter: Int = 0,
        val pstnSupportFlag: Boolean = false,
        val forceDialPadTab: Boolean = false
) : MavericksState {
    val showDialPadTab = forceDialPadTab || pstnSupportFlag
}

sealed class HomeTab(@StringRes val titleRes: Int) {
    data class RoomList(val displayMode: RoomListDisplayMode) : HomeTab(displayMode.titleRes)
    object DialPad : HomeTab(CommonStrings.call_dial_pad_title)
}
