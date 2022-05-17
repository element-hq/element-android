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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// TODO add unit tests
class LocationLiveMapViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationLiveMapViewState,
        getListOfUserLiveLocationUseCase: GetListOfUserLiveLocationUseCase
) : VectorViewModel<LocationLiveMapViewState, LocationLiveMapAction, LocationLiveMapViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationLiveMapViewModel, LocationLiveMapViewState> {
        override fun create(initialState: LocationLiveMapViewState): LocationLiveMapViewModel
    }

    companion object : MavericksViewModelFactory<LocationLiveMapViewModel, LocationLiveMapViewState> by hiltMavericksViewModelFactory()

    /**
     * Map to keep track of symbol ids associated to each user Id.
     */
    val mapSymbolIds = mutableMapOf<String, Long>()

    init {
        getListOfUserLiveLocationUseCase.execute(initialState.roomId)
                .onEach { setState { copy(userLocations = it) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: LocationLiveMapAction) {
        // do nothing, no action for now
    }
}
