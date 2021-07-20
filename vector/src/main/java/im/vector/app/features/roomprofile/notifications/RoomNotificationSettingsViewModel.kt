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
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.rx.rx

class RoomNotificationSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomNotificationSettingsViewState,
        private val room: Room
) : VectorViewModel<RoomNotificationSettingsViewState, RoomNotificationSettingsAction, RoomNotificationSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel
    }

    companion object : MvRxViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel? {
            val fragment: RoomNotificationSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomNotificationSettingsViewModel.create(state)
        }
    }

    init {
        observeNotificationState()
    }

    private fun observeNotificationState() {
        room.rx()
                .liveNotificationState()
                .subscribe{
                    setState {
                        copy(notificationState = it )
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomNotificationSettingsAction) {
        when (action) {
            is RoomNotificationSettingsAction.SelectNotificationState -> handleSelectNotificationState(action)
            is RoomNotificationSettingsAction.Save -> handleSaveNotificationSelection(action)
        }
    }

    private fun handleSelectNotificationState(action: RoomNotificationSettingsAction.SelectNotificationState) {
        setState {
            copy(notificationState = action.notificationState)
        }
    }

    private fun handleSaveNotificationSelection(action: RoomNotificationSettingsAction.Save) {
        setState { copy(isLoading = true) }
        withState { state ->
            viewModelScope.launch {
                runCatching {  room.setRoomNotificationState(state.notificationState) }
                        .onFailure { _viewEvents.post(RoomNotificationSettingsViewEvents.Failure(it)) }
                setState {
                    copy(isLoading = false)
                }
                _viewEvents.post(RoomNotificationSettingsViewEvents.SaveComplete)
            }
        }
    }
}
