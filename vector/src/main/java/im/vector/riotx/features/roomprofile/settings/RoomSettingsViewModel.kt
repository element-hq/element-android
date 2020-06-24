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

package im.vector.riotx.features.roomprofile.settings

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.powerlevels.PowerLevelsHelper
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.powerlevel.PowerLevelsObservableFactory
import io.reactivex.Completable
import io.reactivex.Observable
import java.util.Locale

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
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    val roomSummary = async.invoke()
                    copy(
                            historyVisibilityEvent = room.getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY),
                            roomSummary = async,
                            newName = roomSummary?.displayName,
                            newTopic = roomSummary?.topic
                    )
                }

        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive.subscribe {
            val powerLevelsHelper = PowerLevelsHelper(it)
            val permissions = RoomSettingsViewState.ActionPermissions(
                    canChangeName = powerLevelsHelper.isUserAbleToChangeRoomName(session.myUserId),
                    canChangeTopic = powerLevelsHelper.isUserAbleToChangeRoomTopic(session.myUserId),
                    canChangeCanonicalAlias = powerLevelsHelper.isUserAbleToChangeRoomCanonicalAlias(session.myUserId),
                    canChangeHistoryReadability = powerLevelsHelper.isUserAbleToChangeRoomHistoryReadability(session.myUserId)
            )
            setState { copy(actionPermissions = permissions) }
        }.disposeOnClear()
    }

    override fun handle(action: RoomSettingsAction) {
        when (action) {
            is RoomSettingsAction.EnableEncryption         -> handleEnableEncryption()
            is RoomSettingsAction.SetRoomName              -> {
                setState { copy(newName = action.newName) }
                setState { copy(showSaveAction = shouldShowSaveAction(this)) }
            }
            is RoomSettingsAction.SetRoomTopic             -> {
                setState { copy(newTopic = action.newTopic) }
                setState { copy(showSaveAction = shouldShowSaveAction(this)) }
            }
            is RoomSettingsAction.SetRoomHistoryVisibility -> {
                setState { copy(newHistoryVisibility = action.visibility) }
                setState { copy(showSaveAction = shouldShowSaveAction(this)) }
            }
            is RoomSettingsAction.SetRoomAlias -> {
                setState { copy(newAlias = action.alias) }
                setState { copy(showSaveAction = shouldShowSaveAction(this)) }
            }
            is RoomSettingsAction.Save                     -> saveSettings()
        }
    }

    private fun shouldShowSaveAction(state: RoomSettingsViewState): Boolean {
        val summary = state.roomSummary.invoke()
        return summary?.displayName != state.newName ||
                summary?.topic != state.newTopic ||
                summary?.canonicalAlias != state.newAlias ||
                state.newHistoryVisibility != null
    }

    private fun saveSettings() = withState { state ->
        postLoading(true)

        val operationList = mutableListOf<Completable>()

        val summary = state.roomSummary.invoke()

        if (summary?.displayName != state.newName) {
            operationList.add(room.rx().updateName(state.newName ?: ""))
        }
        if (summary?.topic != state.newTopic) {
            operationList.add(room.rx().updateTopic(state.newTopic ?: ""))
        }

        if (summary?.canonicalAlias != state.newAlias) {
            operationList.add(room.rx().updateCanonicalAlias(state.newAlias ?: ""))
        }

        if (state.newHistoryVisibility != null) {
            operationList.add(room.rx().updateHistoryReadability(state.newHistoryVisibility.name.toLowerCase(Locale.ROOT)))
        }

        Observable
                .fromIterable(operationList)
                .flatMapCompletable { it }
                .subscribe(
                        {
                            postLoading(false)
                            setState { copy(newHistoryVisibility = null) }
                            setState { copy(showSaveAction = shouldShowSaveAction(this)) }
                            _viewEvents.post(RoomSettingsViewEvents.Success)
                        },
                        {
                            postLoading(false)
                            _viewEvents.post(RoomSettingsViewEvents.Failure(it))
                        }
                )
    }

    private fun handleEnableEncryption() {
        postLoading(true)

        room.enableEncryption(callback = object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                postLoading(false)
                _viewEvents.post(RoomSettingsViewEvents.Failure(failure))
            }

            override fun onSuccess(data: Unit) {
                postLoading(false)
            }
        })
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
