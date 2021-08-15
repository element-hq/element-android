/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.RoomGroupingMethod
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.util.MatrixItem

data class HomeDetailViewState(
        val roomGroupingMethod: RoomGroupingMethod = RoomGroupingMethod.BySpace(null),
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
        val syncState: SyncState = SyncState.Idle,
        val showDialPadTab: Boolean = false
) : MvRxState

sealed class HomeTab(@StringRes val titleRes: Int) {
    data class RoomList(val displayMode: RoomListDisplayMode) : HomeTab(displayMode.titleRes)
    object DialPad : HomeTab(R.string.call_dial_pad_title)
}
