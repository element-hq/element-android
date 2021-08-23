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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
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

    @AssistedFactory
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
        fetchRoomDirectoryVisibility()
    }

    private fun fetchRoomDirectoryVisibility() {
        setState {
            copy(
                    roomDirectoryVisibility = Loading()
            )
        }
        viewModelScope.launch {
            runCatching {
                session.getRoomDirectoryVisibility(room.roomId)
            }.fold(
                    {
                        setState {
                            copy(
                                    roomDirectoryVisibility = Success(it)
                            )
                        }
                    },
                    {
                        setState {
                            copy(
                                    roomDirectoryVisibility = Fail(it)
                            )
                        }
                    }
            )
        }
    }

    private fun initHomeServerName() {
        setState {
            copy(
                    homeServerName = session.myUserId.getDomain()
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
                                setState { copy(localAliases = Success(it.sorted())) }
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
                            canChangeCanonicalAlias = powerLevelsHelper.isUserAllowedToSend(
                                    userId = session.myUserId,
                                    isState = true,
                                    eventType = EventType.STATE_ROOM_CANONICAL_ALIAS
                            )
                    )
                    setState {
                        val newPublishManuallyState = if (permissions.canChangeCanonicalAlias) {
                            when (publishManuallyState) {
                                RoomAliasViewState.AddAliasState.Hidden -> RoomAliasViewState.AddAliasState.Closed
                                else                                    -> publishManuallyState
                            }
                        } else {
                            RoomAliasViewState.AddAliasState.Hidden
                        }
                        copy(
                                actionPermissions = permissions,
                                publishManuallyState = newPublishManuallyState
                        )
                    }
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
                                alternativeAliases = it.alternativeAliases.orEmpty().sorted()
                        )
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomAliasAction) {
        when (action) {
            RoomAliasAction.ToggleManualPublishForm       -> handleToggleManualPublishForm()
            is RoomAliasAction.SetNewAlias                -> handleSetNewAlias(action)
            is RoomAliasAction.ManualPublishAlias         -> handleManualPublishAlias()
            is RoomAliasAction.UnpublishAlias             -> handleUnpublishAlias(action)
            is RoomAliasAction.SetCanonicalAlias          -> handleSetCanonicalAlias(action)
            is RoomAliasAction.SetRoomDirectoryVisibility -> handleSetRoomDirectoryVisibility(action)
            RoomAliasAction.ToggleAddLocalAliasForm       -> handleToggleAddLocalAliasForm()
            is RoomAliasAction.SetNewLocalAliasLocalPart  -> handleSetNewLocalAliasLocalPart(action)
            RoomAliasAction.AddLocalAlias                 -> handleAddLocalAlias()
            is RoomAliasAction.RemoveLocalAlias           -> handleRemoveLocalAlias(action)
            is RoomAliasAction.PublishAlias               -> handlePublishAlias(action)
            RoomAliasAction.Retry                         -> handleRetry()
        }.exhaustive
    }

    private fun handleRetry() = withState { state ->
        if (state.localAliases is Fail) {
            fetchRoomAlias()
        }
        if (state.roomDirectoryVisibility is Fail) {
            fetchRoomDirectoryVisibility()
        }
    }

    private fun handleSetRoomDirectoryVisibility(action: RoomAliasAction.SetRoomDirectoryVisibility) {
        postLoading(true)
        viewModelScope.launch {
            runCatching {
                session.setRoomDirectoryVisibility(room.roomId, action.roomDirectoryVisibility)
            }.fold(
                    {
                        setState {
                            copy(
                                    isLoading = false,
                                    // Local echo, no need to fetch the data from the server again
                                    roomDirectoryVisibility = Success(action.roomDirectoryVisibility)
                            )
                        }
                    },
                    {
                        postLoading(false)
                        _viewEvents.post(RoomAliasViewEvents.Failure(it))
                    }
            )
        }
    }

    private fun handleToggleAddLocalAliasForm() {
        setState {
            copy(
                    newLocalAliasState = when (newLocalAliasState) {
                        RoomAliasViewState.AddAliasState.Hidden     -> RoomAliasViewState.AddAliasState.Hidden
                        RoomAliasViewState.AddAliasState.Closed     -> RoomAliasViewState.AddAliasState.Editing("", Uninitialized)
                        is RoomAliasViewState.AddAliasState.Editing -> RoomAliasViewState.AddAliasState.Closed
                    }
            )
        }
    }

    private fun handleToggleManualPublishForm() {
        setState {
            copy(
                    publishManuallyState = when (publishManuallyState) {
                        RoomAliasViewState.AddAliasState.Hidden     -> RoomAliasViewState.AddAliasState.Hidden
                        RoomAliasViewState.AddAliasState.Closed     -> RoomAliasViewState.AddAliasState.Editing("", Uninitialized)
                        is RoomAliasViewState.AddAliasState.Editing -> RoomAliasViewState.AddAliasState.Closed
                    }
            )
        }
    }

    private fun handleSetNewAlias(action: RoomAliasAction.SetNewAlias) {
        setState {
            copy(
                    publishManuallyState = RoomAliasViewState.AddAliasState.Editing(action.alias, Uninitialized)
            )
        }
    }

    private fun handleSetNewLocalAliasLocalPart(action: RoomAliasAction.SetNewLocalAliasLocalPart) {
        setState {
            copy(
                    newLocalAliasState = RoomAliasViewState.AddAliasState.Editing(action.aliasLocalPart, Uninitialized)
            )
        }
    }

    private fun handleManualPublishAlias() = withState { state ->
        val newAlias = (state.publishManuallyState as? RoomAliasViewState.AddAliasState.Editing)?.value ?: return@withState
        updateCanonicalAlias(
                canonicalAlias = state.canonicalAlias,
                alternativeAliases = state.alternativeAliases + newAlias,
                closeForm = true
        )
    }

    private fun handlePublishAlias(action: RoomAliasAction.PublishAlias) = withState { state ->
        updateCanonicalAlias(
                canonicalAlias = state.canonicalAlias,
                alternativeAliases = state.alternativeAliases + action.alias,
                closeForm = false
        )
    }

    private fun handleUnpublishAlias(action: RoomAliasAction.UnpublishAlias) = withState { state ->
        updateCanonicalAlias(
                // We can also unpublish the canonical alias
                canonicalAlias = state.canonicalAlias.takeIf { it != action.alias },
                alternativeAliases = state.alternativeAliases - action.alias,
                closeForm = false
        )
    }

    private fun handleSetCanonicalAlias(action: RoomAliasAction.SetCanonicalAlias) = withState { state ->
        updateCanonicalAlias(
                canonicalAlias = action.canonicalAlias,
                // Ensure the previous canonical alias is moved to the alt aliases
                alternativeAliases = state.allPublishedAliases,
                closeForm = false
        )
    }

    private fun updateCanonicalAlias(canonicalAlias: String?, alternativeAliases: List<String>, closeForm: Boolean) {
        postLoading(true)
        viewModelScope.launch {
            try {
                room.updateCanonicalAlias(canonicalAlias, alternativeAliases)
                setState {
                    copy(
                            isLoading = false,
                            publishManuallyState = if (closeForm) RoomAliasViewState.AddAliasState.Closed else publishManuallyState
                    )
                }
            } catch (failure: Throwable) {
                postLoading(false)
                _viewEvents.post(RoomAliasViewEvents.Failure(failure))
            }
        }
    }

    private fun handleAddLocalAlias() = withState { state ->
        val previousState = (state.newLocalAliasState as? RoomAliasViewState.AddAliasState.Editing) ?: return@withState

        setState {
            copy(
                    isLoading = true,
                    newLocalAliasState = previousState.copy(asyncRequest = Loading())
            )
        }
        viewModelScope.launch {
            runCatching { room.addAlias(previousState.value) }
                    .onFailure {
                        setState {
                            copy(
                                    isLoading = false,
                                    newLocalAliasState = previousState.copy(asyncRequest = Fail(it))
                            )
                        }
                        _viewEvents.post(RoomAliasViewEvents.Failure(it))
                    }
                    .onSuccess {
                        setState {
                            copy(
                                    isLoading = false,
                                    newLocalAliasState = RoomAliasViewState.AddAliasState.Closed,
                                    // Local echo
                                    localAliases = Success((localAliases().orEmpty() + previousState.value).sorted())
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
                    .onFailure {
                        setState {
                            copy(isLoading = false)
                        }
                        _viewEvents.post(RoomAliasViewEvents.Failure(it))
                    }
                    .onSuccess {
                        // Local echo
                        setState {
                            copy(
                                    isLoading = false,
                                    // Local echo
                                    localAliases = Success(localAliases().orEmpty() - action.alias)
                            )
                        }
                        fetchRoomAlias()
                    }
        }
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
