/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.location.live.StopLiveLocationShareUseCase
import im.vector.app.features.location.live.tracking.LocationSharingServiceConnection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult

class LiveLocationMapViewModel @AssistedInject constructor(
        @Assisted private val initialState: LiveLocationMapViewState,
        getListOfUserLiveLocationUseCase: GetListOfUserLiveLocationUseCase,
        private val locationSharingServiceConnection: LocationSharingServiceConnection,
        private val stopLiveLocationShareUseCase: StopLiveLocationShareUseCase,
) : VectorViewModel<LiveLocationMapViewState, LiveLocationMapAction, LiveLocationMapViewEvents>(initialState), LocationSharingServiceConnection.Callback {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LiveLocationMapViewModel, LiveLocationMapViewState> {
        override fun create(initialState: LiveLocationMapViewState): LiveLocationMapViewModel
    }

    companion object : MavericksViewModelFactory<LiveLocationMapViewModel, LiveLocationMapViewState> by hiltMavericksViewModelFactory()

    init {
        getListOfUserLiveLocationUseCase.execute(initialState.roomId)
                .onEach { setState { copy(userLocations = it) } }
                .launchIn(viewModelScope)
        locationSharingServiceConnection.bind(this)
    }

    override fun onCleared() {
        locationSharingServiceConnection.unbind(this)
        super.onCleared()
    }

    override fun handle(action: LiveLocationMapAction) {
        when (action) {
            is LiveLocationMapAction.AddMapSymbol -> handleAddMapSymbol(action)
            is LiveLocationMapAction.RemoveMapSymbol -> handleRemoveMapSymbol(action)
            LiveLocationMapAction.StopSharing -> handleStopSharing()
            LiveLocationMapAction.ShowMapLoadingError -> handleShowMapLoadingError()
        }
    }

    private fun handleAddMapSymbol(action: LiveLocationMapAction.AddMapSymbol) = withState { state ->
        val newMapSymbolIds = state.mapSymbolIds.toMutableMap().apply { set(action.key, action.value) }
        setState {
            copy(mapSymbolIds = newMapSymbolIds)
        }
    }

    private fun handleRemoveMapSymbol(action: LiveLocationMapAction.RemoveMapSymbol) = withState { state ->
        val newMapSymbolIds = state.mapSymbolIds.toMutableMap().apply { remove(action.key) }
        setState {
            copy(mapSymbolIds = newMapSymbolIds)
        }
    }

    private fun handleStopSharing() {
        viewModelScope.launch {
            val result = stopLiveLocationShareUseCase.execute(initialState.roomId)
            if (result is UpdateLiveLocationShareResult.Failure) {
                _viewEvents.post(LiveLocationMapViewEvents.Error(result.error))
            }
        }
    }

    private fun handleShowMapLoadingError() {
        setState { copy(loadingMapHasFailed = true) }
    }

    override fun onLocationServiceRunning(roomIds: Set<String>) {
        // NOOP
    }

    override fun onLocationServiceStopped() {
        // NOOP
    }

    override fun onLocationServiceError(error: Throwable) {
        _viewEvents.post(LiveLocationMapViewEvents.Error(error))
    }
}
