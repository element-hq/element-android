/*
 * Copyright (c) 2022 New Vector Ltd
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
