/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomPermissionsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomPermissionsViewState,
        private val session: Session
) :
        VectorViewModel<RoomPermissionsViewState, RoomPermissionsAction, RoomPermissionsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomPermissionsViewModel, RoomPermissionsViewState> {
        override fun create(initialState: RoomPermissionsViewState): RoomPermissionsViewModel
    }

    companion object : MavericksViewModelFactory<RoomPermissionsViewModel, RoomPermissionsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observePowerLevel()
    }

    private fun observeRoomSummary() {
        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(
                            roomSummary = async
                    )
                }
    }

    private fun observePowerLevel() {
        room.flow().liveRoomPowerLevels()
                .onEach { roomPowerLevels ->
                    val permissions = RoomPermissionsViewState.ActionPermissions(
                            canChangePowerLevels = roomPowerLevels.isUserAllowedToSend(
                                    userId = session.myUserId,
                                    isState = true,
                                    eventType = EventType.STATE_ROOM_POWER_LEVELS
                            )
                    )
                    val powerLevelsContent  = roomPowerLevels.powerLevelsContent
                    setState {
                        copy(
                                actionPermissions = permissions,
                                currentPowerLevelsContent = if (powerLevelsContent != null) {
                                    Success(powerLevelsContent)
                                } else {
                                    Uninitialized
                                }
                        )
                    }
                }.launchIn(viewModelScope)
    }

    override fun handle(action: RoomPermissionsAction) {
        when (action) {
            is RoomPermissionsAction.UpdatePermission -> updatePermission(action)
            RoomPermissionsAction.ToggleShowAllPermissions -> toggleShowAllPermissions()
        }
    }

    private fun toggleShowAllPermissions() {
        setState {
            copy(showAdvancedPermissions = !showAdvancedPermissions)
        }
    }

    private fun updatePermission(action: RoomPermissionsAction.UpdatePermission) {
        withState { state ->
            val currentPowerLevelsContent = state.currentPowerLevelsContent.invoke() ?: return@withState
            postLoading(true)
            viewModelScope.launch {
                try {
                    val newPowerLevelsContent = when (action.editablePermission) {
                        is EditablePermission.EventTypeEditablePermission -> currentPowerLevelsContent.copy(
                                events = currentPowerLevelsContent.events.orEmpty().toMutableMap().apply {
                                    put(action.editablePermission.eventType, action.powerLevel.value)
                                }
                        )
                        is EditablePermission.DefaultRole -> currentPowerLevelsContent.copy(usersDefault = action.powerLevel.value)
                        is EditablePermission.SendMessages -> currentPowerLevelsContent.copy(eventsDefault = action.powerLevel.value)
                        is EditablePermission.InviteUsers -> currentPowerLevelsContent.copy(invite = action.powerLevel.value)
                        is EditablePermission.ChangeSettings -> currentPowerLevelsContent.copy(stateDefault = action.powerLevel.value)
                        is EditablePermission.KickUsers -> currentPowerLevelsContent.copy(kick = action.powerLevel.value)
                        is EditablePermission.BanUsers -> currentPowerLevelsContent.copy(ban = action.powerLevel.value)
                        is EditablePermission.RemoveMessagesSentByOthers -> currentPowerLevelsContent.copy(redact = action.powerLevel.value)
                        is EditablePermission.NotifyEveryone -> currentPowerLevelsContent.copy(
                                notifications = currentPowerLevelsContent.notifications.orEmpty().toMutableMap().apply {
                                    put(PowerLevelsContent.NOTIFICATIONS_ROOM_KEY, action.powerLevel.value)
                                }
                        )
                    }
                    room.stateService().sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent.toContent())
                    setState {
                        copy(
                                isLoading = false
                        )
                    }
                } catch (failure: Throwable) {
                    postLoading(false)
                    _viewEvents.post(RoomPermissionsViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
