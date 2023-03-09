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

package im.vector.app.features.location.preview

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.LocationTracker
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class LocationPreviewViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationPreviewViewState,
        private val session: Session,
        private val locationPinProvider: LocationPinProvider,
        private val locationTracker: LocationTracker,
) : VectorViewModel<LocationPreviewViewState, LocationPreviewAction, LocationPreviewViewEvents>(initialState), LocationTracker.Callback {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationPreviewViewModel, LocationPreviewViewState> {
        override fun create(initialState: LocationPreviewViewState): LocationPreviewViewModel
    }

    companion object : MavericksViewModelFactory<LocationPreviewViewModel, LocationPreviewViewState> by hiltMavericksViewModelFactory()

    init {
        initPin(initialState.pinUserId)
        initLocationTracking()
    }

    private fun initPin(userId: String?) {
        locationPinProvider.create(userId) { pinDrawable ->
            setState { copy(pinDrawable = pinDrawable) }
        }
    }

    private fun initLocationTracking() {
        locationTracker.addCallback(this)
        locationTracker.locations
                .onEach(::onLocationUpdate)
                .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.removeCallback(this)
    }

    override fun handle(action: LocationPreviewAction) {
        when (action) {
            LocationPreviewAction.ShowMapLoadingError -> handleShowMapLoadingError()
            LocationPreviewAction.ZoomToUserLocation -> handleZoomToUserLocationAction()
        }
    }

    private fun handleShowMapLoadingError() {
        setState { copy(loadingMapHasFailed = true) }
    }

    private fun handleZoomToUserLocationAction() = withState { state ->
        if (!state.isLoadingUserLocation) {
            setState {
                copy(isLoadingUserLocation = true)
            }
            viewModelScope.launch(session.coroutineDispatchers.main) {
                locationTracker.start()
                locationTracker.requestLastKnownLocation()
            }
        }
    }

    override fun onNoLocationProviderAvailable() {
        _viewEvents.post(LocationPreviewViewEvents.UserLocationNotAvailableError)
    }

    private fun onLocationUpdate(locationData: LocationData) = withState { state ->
        val zoomToUserLocation = state.isLoadingUserLocation

        setState {
            copy(
                    lastKnownUserLocation = locationData,
                    isLoadingUserLocation = false,
            )
        }

        if (zoomToUserLocation) {
            _viewEvents.post(LocationPreviewViewEvents.ZoomToUserLocation(locationData))
        }
    }
}
