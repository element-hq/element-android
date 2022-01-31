/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.location

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import org.matrix.android.sdk.api.session.Session

class LocationSharingViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationSharingViewState,
        private val locationTracker: LocationTracker,
        private val locationPinProvider: LocationPinProvider,
        private val session: Session
) : VectorViewModel<LocationSharingViewState, LocationSharingAction, LocationSharingViewEvents>(initialState), LocationTracker.Callback {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationSharingViewModel, LocationSharingViewState> {
        override fun create(initialState: LocationSharingViewState): LocationSharingViewModel
    }

    companion object : MavericksViewModelFactory<LocationSharingViewModel, LocationSharingViewState> by hiltMavericksViewModelFactory()

    init {
        locationTracker.start(this)
        createPin()
    }

    private fun createPin() {
        locationPinProvider.create(session.myUserId) {
            setState {
                copy(
                        pinDrawable = it
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stop()
    }

    override fun handle(action: LocationSharingAction) {
        when (action) {
            LocationSharingAction.OnShareLocation -> handleShareLocation()
        }.exhaustive
    }

    private fun handleShareLocation() = withState { state ->
        state.lastKnownLocation?.let { location ->
            room.sendLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    uncertainty = location.uncertainty
            )
            _viewEvents.post(LocationSharingViewEvents.Close)
        } ?: run {
            _viewEvents.post(LocationSharingViewEvents.LocationNotAvailableError)
        }
    }

    override fun onLocationUpdate(locationData: LocationData) {
        setState {
            copy(lastKnownLocation = locationData)
        }
    }

    override fun onLocationProviderIsNotAvailable() {
        _viewEvents.post(LocationSharingViewEvents.LocationNotAvailableError)
    }
}
