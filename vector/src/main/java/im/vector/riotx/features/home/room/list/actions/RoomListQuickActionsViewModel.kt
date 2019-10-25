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
package im.vector.riotx.features.home.room.list.actions

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotx.features.html.EventHtmlRenderer
import timber.log.Timber

class RoomListActionsViewModel @AssistedInject constructor(@Assisted
                                                           initialState: RoomListQuickActionsState,
                                                           session: Session
) : VectorViewModel<RoomListQuickActionsState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomListQuickActionsState): RoomListActionsViewModel
    }

    companion object : MvRxViewModelFactory<RoomListActionsViewModel, RoomListQuickActionsState> {

        override fun create(viewModelContext: ViewModelContext, state: RoomListQuickActionsState): RoomListActionsViewModel? {
            val fragment: RoomListQuickActionsBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomListActionsViewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observeNotificationState()
    }

    private fun observeNotificationState() {
        room
                .rx()
                .liveNotificationState()
                .execute {
                    copy(roomNotificationState = it)
                }
    }

    private fun observeRoomSummary() {
        room
                .rx()
                .liveRoomSummary()
                .unwrap()
                .execute {
                    copy(roomSummary = it)
                }
    }


}
