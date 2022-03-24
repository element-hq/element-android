/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.permissions

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomPermissionsViewModel @AssistedInject constructor(@Assisted initialState: RoomPermissionsViewState,
                                                           private val session: Session) :
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
        PowerLevelsFlowFactory(room)
                .createFlow()
                .onEach { powerLevelContent ->
                    val powerLevelsHelper = PowerLevelsHelper(powerLevelContent)
                    val permissions = RoomPermissionsViewState.ActionPermissions(
                            canChangePowerLevels = powerLevelsHelper.isUserAllowedToSend(
                                    userId = session.myUserId,
                                    isState = true,
                                    eventType = EventType.STATE_ROOM_POWER_LEVELS
                            )
                    )
                    setState {
                        copy(
                                actionPermissions = permissions,
                                currentPowerLevelsContent = Success(powerLevelContent)
                        )
                    }
                }.launchIn(viewModelScope)
    }

    override fun handle(action: RoomPermissionsAction) {
        when (action) {
            is RoomPermissionsAction.UpdatePermission      -> updatePermission(action)
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
            val currentPowerLevel = state.currentPowerLevelsContent.invoke() ?: return@withState
            postLoading(true)
            viewModelScope.launch {
                try {
                    val newPowerLevelsContent = when (action.editablePermission) {
                        is EditablePermission.EventTypeEditablePermission -> currentPowerLevel.copy(
                                events = currentPowerLevel.events.orEmpty().toMutableMap().apply {
                                    put(action.editablePermission.eventType, action.powerLevel)
                                }
                        )
                        is EditablePermission.DefaultRole                 -> currentPowerLevel.copy(usersDefault = action.powerLevel)
                        is EditablePermission.SendMessages                -> currentPowerLevel.copy(eventsDefault = action.powerLevel)
                        is EditablePermission.InviteUsers                 -> currentPowerLevel.copy(invite = action.powerLevel)
                        is EditablePermission.ChangeSettings              -> currentPowerLevel.copy(stateDefault = action.powerLevel)
                        is EditablePermission.KickUsers                   -> currentPowerLevel.copy(kick = action.powerLevel)
                        is EditablePermission.BanUsers                    -> currentPowerLevel.copy(ban = action.powerLevel)
                        is EditablePermission.RemoveMessagesSentByOthers  -> currentPowerLevel.copy(redact = action.powerLevel)
                        is EditablePermission.NotifyEveryone              -> currentPowerLevel.copy(
                                notifications = currentPowerLevel.notifications.orEmpty().toMutableMap().apply {
                                    put(PowerLevelsContent.NOTIFICATIONS_ROOM_KEY, action.powerLevel)
                                }
                        )
                    }
                    room.sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent.toContent())
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
