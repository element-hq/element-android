/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings

import androidx.core.net.toFile
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap

class RoomSettingsViewModel @AssistedInject constructor(@Assisted initialState: RoomSettingsViewState,
                                                        private val vectorPreferences: VectorPreferences,
                                                        private val session: Session) :
        VectorViewModel<RoomSettingsViewState, RoomSettingsAction, RoomSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> {
        override fun create(initialState: RoomSettingsViewState): RoomSettingsViewModel
    }

    companion object : MavericksViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observeRoomHistoryVisibility()
        observeJoinRule()
        observeGuestAccess()
        observeRoomAvatar()
        observeState()

        val homeServerCapabilities = session.getHomeServerCapabilities()
        val canUseRestricted = homeServerCapabilities
                .isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED, room.getRoomVersion())

        val restrictedSupport = homeServerCapabilities.isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED)
        val couldUpgradeToRestricted = restrictedSupport == HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED

        setState {
            copy(
                    supportsRestricted = canUseRestricted,
                    canUpgradeToRestricted = couldUpgradeToRestricted
            )
        }
    }

    private fun observeState() {
        onEach(
                RoomSettingsViewState::avatarAction,
                RoomSettingsViewState::newName,
                RoomSettingsViewState::newTopic,
                RoomSettingsViewState::newHistoryVisibility,
                RoomSettingsViewState::newRoomJoinRules,
                RoomSettingsViewState::roomSummary) { avatarAction,
                                                      newName,
                                                      newTopic,
                                                      newHistoryVisibility,
                                                      newJoinRule,
                                                      asyncSummary ->
            val summary = asyncSummary()
            setState {
                copy(
                        showSaveAction = avatarAction !is RoomSettingsViewState.AvatarAction.None ||
                                summary?.name != newName ||
                                summary?.topic != newTopic ||
                                (newHistoryVisibility != null && newHistoryVisibility != currentHistoryVisibility) ||
                                newJoinRule.hasChanged()
                )
            }
        }
    }

    private fun observeRoomSummary() {
        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    val roomSummary = async.invoke()
                    copy(
                            roomSummary = async,
                            newName = roomSummary?.name,
                            newTopic = roomSummary?.topic
                    )
                }

        val powerLevelsContentLive = PowerLevelsFlowFactory(room).createFlow()

        powerLevelsContentLive
                .onEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomSettingsViewState.ActionPermissions(
                            canChangeAvatar = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_AVATAR),
                            canChangeName = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_NAME),
                            canChangeTopic = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_TOPIC),
                            canChangeHistoryVisibility = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_HISTORY_VISIBILITY),
                            canChangeJoinRule = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_JOIN_RULES) &&
                                    powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                            EventType.STATE_ROOM_GUEST_ACCESS),
                            canAddChildren = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_SPACE_CHILD)
                    )
                    setState {
                        copy(actionPermissions = permissions)
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeRoomHistoryVisibility() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomHistoryVisibilityContent>() }
                .unwrap()
                .mapNotNull { it.historyVisibility }
                .setOnEach {
                    copy(currentHistoryVisibility = it)
                }
    }

    private fun observeJoinRule() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomJoinRulesContent>() }
                .unwrap()
                .mapNotNull { it.joinRules }
                .setOnEach {
                    copy(currentRoomJoinRules = it)
                }
    }

    private fun observeGuestAccess() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_GUEST_ACCESS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomGuestAccessContent>() }
                .unwrap()
                .mapNotNull { it.guestAccess }
                .setOnEach {
                    copy(currentGuestAccess = it)
                }
    }

    /**
     * We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
     */
    private fun observeRoomAvatar() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_AVATAR, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomAvatarContent>() }
                .unwrap()
                .setOnEach {
                    copy(currentRoomAvatarUrl = it.avatarUrl)
                }
    }

    override fun handle(action: RoomSettingsAction) {
        when (action) {
            is RoomSettingsAction.SetAvatarAction          -> handleSetAvatarAction(action)
            is RoomSettingsAction.SetRoomName              -> setState { copy(newName = action.newName) }
            is RoomSettingsAction.SetRoomTopic             -> setState { copy(newTopic = action.newTopic) }
            is RoomSettingsAction.SetRoomHistoryVisibility -> setState { copy(newHistoryVisibility = action.visibility) }
            is RoomSettingsAction.SetRoomJoinRule          -> handleSetRoomJoinRule(action)
            is RoomSettingsAction.SetRoomGuestAccess       -> handleSetGuestAccess(action)
            is RoomSettingsAction.Save                     -> saveSettings()
            is RoomSettingsAction.Cancel                   -> cancel()
        }
    }

    private fun handleSetRoomJoinRule(action: RoomSettingsAction.SetRoomJoinRule) = withState { state ->
        setState {
            copy(newRoomJoinRules = RoomSettingsViewState.NewJoinRule(
                    newJoinRules = action.roomJoinRule.takeIf { it != state.currentRoomJoinRules },
                    newGuestAccess = state.newRoomJoinRules.newGuestAccess.takeIf { it != state.currentGuestAccess }
            ))
        }
    }

    private fun handleSetGuestAccess(action: RoomSettingsAction.SetRoomGuestAccess) = withState { state ->
        setState {
            copy(newRoomJoinRules = RoomSettingsViewState.NewJoinRule(
                    newJoinRules = state.newRoomJoinRules.newJoinRules.takeIf { it != state.currentRoomJoinRules },
                    newGuestAccess = action.guestAccess.takeIf { it != state.currentGuestAccess }
            ))
        }
    }

    private fun handleSetAvatarAction(action: RoomSettingsAction.SetAvatarAction) {
        setState {
            deletePendingAvatar(this)
            copy(avatarAction = action.avatarAction)
        }
    }

    private fun deletePendingAvatar(state: RoomSettingsViewState) {
        // Maybe delete the pending avatar
        (state.avatarAction as? RoomSettingsViewState.AvatarAction.UpdateAvatar)
                ?.let { tryOrNull { it.newAvatarUri.toFile().delete() } }
    }

    private fun cancel() {
        withState { deletePendingAvatar(it) }

        _viewEvents.post(RoomSettingsViewEvents.GoBack)
    }

    private fun saveSettings() = withState { state ->
        val operationList = mutableListOf<suspend () -> Unit>()

        val summary = state.roomSummary.invoke()

        when (val avatarAction = state.avatarAction) {
            RoomSettingsViewState.AvatarAction.None            -> Unit
            RoomSettingsViewState.AvatarAction.DeleteAvatar    -> {
                operationList.add { room.deleteAvatar() }
            }
            is RoomSettingsViewState.AvatarAction.UpdateAvatar -> {
                operationList.add { room.updateAvatar(avatarAction.newAvatarUri, avatarAction.newAvatarFileName) }
            }
        }
        if (summary?.name != state.newName) {
            operationList.add { room.updateName(state.newName ?: "") }
        }
        if (summary?.topic != state.newTopic) {
            operationList.add { room.updateTopic(state.newTopic ?: "") }
        }

        if (state.newHistoryVisibility != null) {
            operationList.add { room.updateHistoryReadability(state.newHistoryVisibility) }
        }

        if (state.newRoomJoinRules.hasChanged()) {
            operationList.add { room.updateJoinRule(state.newRoomJoinRules.newJoinRules, state.newRoomJoinRules.newGuestAccess) }
        }
        viewModelScope.launch {
            updateLoadingState(isLoading = true)
            try {
                for (operation in operationList) {
                    operation.invoke()
                }
                setState {
                    deletePendingAvatar(this)
                    copy(
                            avatarAction = RoomSettingsViewState.AvatarAction.None,
                            newHistoryVisibility = null,
                            newRoomJoinRules = RoomSettingsViewState.NewJoinRule()
                    )
                }
                _viewEvents.post(RoomSettingsViewEvents.Success)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomSettingsViewEvents.Failure(failure))
            } finally {
                updateLoadingState(isLoading = false)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
