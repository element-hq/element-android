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
package im.vector.app.features.home.room.list.actions

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class RoomListQuickActionsViewModel @AssistedInject constructor(@Assisted initialState: RoomListQuickActionsState,
                                                                session: Session
) : VectorViewModel<RoomListQuickActionsState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomListQuickActionsState): RoomListQuickActionsViewModel
    }

    companion object : MvRxViewModelFactory<RoomListQuickActionsViewModel, RoomListQuickActionsState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListQuickActionsState): RoomListQuickActionsViewModel? {
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

    override fun handle(action: EmptyAction) {
        // No op
    }
}
