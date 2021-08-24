/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.notifications

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class RoomNotificationSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomNotificationSettingsViewState,
        session: Session
) : VectorViewModel<RoomNotificationSettingsViewState, RoomNotificationSettingsAction, RoomNotificationSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel
    }

    companion object : MvRxViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel {
            val fragmentModelContext = (viewModelContext as FragmentViewModelContext)
            return if (fragmentModelContext.fragment is RoomNotificationSettingsFragment) {
                val fragment: RoomNotificationSettingsFragment = fragmentModelContext.fragment()
                fragment.viewModelFactory.create(state)
            } else {
                val fragment: RoomListQuickActionsBottomSheet = fragmentModelContext.fragment()
                fragment.roomNotificationSettingsViewModelFactory.create(state)
            }
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeSummary()
        observeNotificationState()
    }

    private fun observeSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observeNotificationState() {
        room.rx()
                .liveNotificationState()
                .execute {
                    copy(notificationState = it)
                }
    }

    override fun handle(action: RoomNotificationSettingsAction) {
        when (action) {
            is RoomNotificationSettingsAction.SelectNotificationState -> handleSelectNotificationState(action)
        }
    }

    private fun handleSelectNotificationState(action: RoomNotificationSettingsAction.SelectNotificationState) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {  room.setRoomNotificationState(action.notificationState) }
                    .fold(
                            {
                                setState {
                                    copy(isLoading = false, notificationState = Success(action.notificationState))
                                }
                            },
                            {
                                setState {
                                    copy(isLoading = false)
                                }
                                _viewEvents.post(RoomNotificationSettingsViewEvents.Failure(it))
                            }
                    )
        }
    }
}
