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

package im.vector.app.features.roomprofile.alias

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.rx.mapOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class RoomAliasViewModel @AssistedInject constructor(@Assisted initialState: RoomAliasViewState,
                                                     private val session: Session)
    : VectorViewModel<RoomAliasViewState, RoomAliasAction, RoomAliasViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomAliasViewState): RoomAliasViewModel
    }

    companion object : MvRxViewModelFactory<RoomAliasViewModel, RoomAliasViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomAliasViewState): RoomAliasViewModel? {
            val fragment: RoomAliasFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        initHomeServerName()
        observeRoomSummary()
        observePowerLevel()
        observeRoomCanonicalAlias()
        fetchRoomAlias()
    }

    private fun initHomeServerName() {
        setState {
            copy(
                    homeServerName = session.myUserId.substringAfter(":")
            )
        }
    }

    private fun fetchRoomAlias() {
        setState {
            copy(
                    localAliases = Loading()
            )
        }

        viewModelScope.launch {
            runCatching { room.getRoomAliases() }
                    .fold(
                            {
                                setState { copy(localAliases = Success(it)) }
                            },
                            {
                                setState { copy(localAliases = Fail(it)) }
                            }
                    )
        }
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(
                            roomSummary = async
                    )
                }
    }

    private fun observePowerLevel() {
        PowerLevelsObservableFactory(room)
                .createObservable()
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomAliasViewState.ActionPermissions(
                            canChangeCanonicalAlias = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_CANONICAL_ALIAS),
                    )
                    setState { copy(actionPermissions = permissions) }
                }
                .disposeOnClear()
    }

    /**
     * We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
     */
    private fun observeRoomCanonicalAlias() {
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_CANONICAL_ALIAS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomCanonicalAliasContent>() }
                .unwrap()
                .subscribe {
                    setState {
                        copy(
                                canonicalAlias = it.canonicalAlias,
                                alternativeAliases = it.alternativeAliases.orEmpty()
                        )
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomAliasAction) {
        when (action) {
            is RoomAliasAction.AddAlias -> handleAddAlias()
            is RoomAliasAction.RemoveAlias -> handleRemoveAlias(action)
            is RoomAliasAction.SetCanonicalAlias -> handleSetCanonicalAlias(action)
            RoomAliasAction.UnSetCanonicalAlias -> handleUnsetCanonicalAlias()
            is RoomAliasAction.SetNewLocalAliasLocalPart -> handleSetNewLocalAliasLocalPart(action)
            RoomAliasAction.AddLocalAlias -> handleAddLocalAlias()
            is RoomAliasAction.RemoveLocalAlias -> handleRemoveLocalAlias(action)
        }.exhaustive
    }

    private fun handleSetNewLocalAliasLocalPart(action: RoomAliasAction.SetNewLocalAliasLocalPart) {
        setState {
            copy(
                    newLocalAlias = action.aliasLocalPart,
                    asyncNewLocalAliasRequest = Uninitialized
            )
        }
    }

    private fun handleAddAlias() = withState { state ->
        TODO()
    }

    private fun handleRemoveAlias(action: RoomAliasAction.RemoveAlias) = withState { state ->
        updateCanonicalAlias(
                state.canonicalAlias,
                state.alternativeAliases - action.alias
        )
    }

    private fun handleSetCanonicalAlias(action: RoomAliasAction.SetCanonicalAlias) = withState { state ->
        updateCanonicalAlias(
                action.canonicalAlias,
                state.alternativeAliases
        )
    }

    private fun handleUnsetCanonicalAlias() = withState { state ->
        updateCanonicalAlias(
                null,
                state.alternativeAliases
        )
    }

    private fun updateCanonicalAlias(canonicalAlias: String?, alternativeAliases: List<String>) {
        postLoading(true)
        room.updateCanonicalAlias(canonicalAlias, alternativeAliases, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                postLoading(false)
            }

            override fun onFailure(failure: Throwable) {
                postLoading(false)
                _viewEvents.post(RoomAliasViewEvents.Failure(failure))
            }
        }
        )
    }

    private fun handleAddLocalAlias() = withState { state ->
        setState {
            copy(
                    isLoading = true,
                    asyncNewLocalAliasRequest = Loading()
            )
        }
        viewModelScope.launch {
            runCatching { room.addAlias(state.newLocalAlias) }
                    .onFailure {
                        setState {
                            copy(
                                    isLoading = false,
                                    asyncNewLocalAliasRequest = Fail(it)
                            )
                        }
                        _viewEvents.post(RoomAliasViewEvents.Failure(it))
                    }
                    .onSuccess {
                        setState {
                            copy(
                                    isLoading = false,
                                    asyncNewLocalAliasRequest = Uninitialized
                            )
                        }
                        fetchRoomAlias()
                    }
        }
    }

    private fun handleRemoveLocalAlias(action: RoomAliasAction.RemoveLocalAlias) {
        postLoading(true)
        viewModelScope.launch {
            runCatching { session.deleteRoomAlias(action.alias) }
                    .onFailure { _viewEvents.post(RoomAliasViewEvents.Failure(it)) }
                    .onSuccess { fetchRoomAlias() }
            postLoading(false)
        }
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
