/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.notifications

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomNotificationSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomNotificationSettingsViewState,
        session: Session
) : VectorViewModel<RoomNotificationSettingsViewState, RoomNotificationSettingsAction, RoomNotificationSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> {
        override fun create(initialState: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel
    }

    companion object : MavericksViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeSummary()
        observeNotificationState()
    }

    private fun observeSummary() {
        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observeNotificationState() {
        room.flow()
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
            runCatching { room.roomPushRuleService().setRoomNotificationState(action.notificationState) }
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
