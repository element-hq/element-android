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
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import io.reactivex.Completable
import io.reactivex.Observable
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.rx.mapOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class RoomSettingsViewModel @AssistedInject constructor(@Assisted initialState: RoomSettingsViewState,
                                                        private val session: Session)
    : VectorViewModel<RoomSettingsViewState, RoomSettingsAction, RoomSettingsViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomSettingsViewState): RoomSettingsViewModel
    }

    companion object : MvRxViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomSettingsViewState): RoomSettingsViewModel? {
            val fragment: RoomSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observeRoomAvatar()
        observeState()
    }

    private fun observeState() {
        selectSubscribe(
                RoomSettingsViewState::avatarAction,
                RoomSettingsViewState::newName,
                RoomSettingsViewState::newCanonicalAlias,
                RoomSettingsViewState::newTopic,
                RoomSettingsViewState::newHistoryVisibility,
                RoomSettingsViewState::roomSummary) { avatarAction,
                                                      newName,
                                                      newCanonicalAlias,
                                                      newTopic,
                                                      newHistoryVisibility,
                                                      asyncSummary ->
            val summary = asyncSummary()
            setState {
                copy(
                        showSaveAction = avatarAction !is RoomSettingsViewState.AvatarAction.None
                                || summary?.name != newName
                                || summary?.topic != newTopic
                                || summary?.canonicalAlias != newCanonicalAlias?.takeIf { it.isNotEmpty() }
                                || newHistoryVisibility != null
                )
            }
        }
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    val roomSummary = async.invoke()
                    copy(
                            historyVisibilityEvent = room.getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY),
                            roomSummary = async,
                            newName = roomSummary?.name,
                            newTopic = roomSummary?.topic,
                            newCanonicalAlias = roomSummary?.canonicalAlias
                    )
                }

        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomSettingsViewState.ActionPermissions(
                            canChangeAvatar = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_AVATAR),
                            canChangeName = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_NAME),
                            canChangeTopic = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_TOPIC),
                            canChangeCanonicalAlias = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_CANONICAL_ALIAS),
                            canChangeHistoryReadability = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_HISTORY_VISIBILITY)
                    )
                    setState { copy(actionPermissions = permissions) }
                }
                .disposeOnClear()
    }

    /**
     * We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
     */
    private fun observeRoomAvatar() {
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_AVATAR, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomAvatarContent>() }
                .unwrap()
                .subscribe {
                    setState { copy(currentRoomAvatarUrl = it.avatarUrl) }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomSettingsAction) {
        when (action) {
            is RoomSettingsAction.SetAvatarAction          -> handleSetAvatarAction(action)
            is RoomSettingsAction.SetRoomName              -> setState { copy(newName = action.newName) }
            is RoomSettingsAction.SetRoomTopic             -> setState { copy(newTopic = action.newTopic) }
            is RoomSettingsAction.SetRoomHistoryVisibility -> setState { copy(newHistoryVisibility = action.visibility) }
            is RoomSettingsAction.SetRoomCanonicalAlias    -> setState { copy(newCanonicalAlias = action.newCanonicalAlias) }
            is RoomSettingsAction.Save                     -> saveSettings()
            is RoomSettingsAction.Cancel                   -> cancel()
        }.exhaustive
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
        postLoading(true)

        val operationList = mutableListOf<Completable>()

        val summary = state.roomSummary.invoke()

        when (val avatarAction = state.avatarAction) {
            RoomSettingsViewState.AvatarAction.None            -> Unit
            RoomSettingsViewState.AvatarAction.DeleteAvatar    -> {
                operationList.add(room.rx().deleteAvatar())
            }
            is RoomSettingsViewState.AvatarAction.UpdateAvatar -> {
                operationList.add(room.rx().updateAvatar(avatarAction.newAvatarUri, avatarAction.newAvatarFileName))
            }
        }
        if (summary?.name != state.newName) {
            operationList.add(room.rx().updateName(state.newName ?: ""))
        }
        if (summary?.topic != state.newTopic) {
            operationList.add(room.rx().updateTopic(state.newTopic ?: ""))
        }

        if (state.newCanonicalAlias != null && summary?.canonicalAlias != state.newCanonicalAlias.takeIf { it.isNotEmpty() }) {
            operationList.add(room.rx().addRoomAlias(state.newCanonicalAlias))
            operationList.add(room.rx().updateCanonicalAlias(state.newCanonicalAlias))
        }

        if (state.newHistoryVisibility != null) {
            operationList.add(room.rx().updateHistoryReadability(state.newHistoryVisibility))
        }

        Observable
                .fromIterable(operationList)
                .concatMapCompletable { it }
                .subscribe(
                        {
                            postLoading(false)
                            setState {
                                deletePendingAvatar(this)
                                copy(
                                        avatarAction = RoomSettingsViewState.AvatarAction.None,
                                        newHistoryVisibility = null
                                )
                            }
                            _viewEvents.post(RoomSettingsViewEvents.Success)
                        },
                        {
                            postLoading(false)
                            _viewEvents.post(RoomSettingsViewEvents.Failure(it))
                        }
                )
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
