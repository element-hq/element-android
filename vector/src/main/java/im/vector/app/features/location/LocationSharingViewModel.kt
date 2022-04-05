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

import android.graphics.drawable.Drawable
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.domain.usecase.CompareLocationsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem

/**
 * Sampling period to compare target location and user location.
 */
private const val TARGET_LOCATION_CHANGE_SAMPLING_PERIOD_IN_MS = 100L

class LocationSharingViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationSharingViewState,
        private val locationTracker: LocationTracker,
        private val locationPinProvider: LocationPinProvider,
        private val session: Session,
        private val compareLocationsUseCase: CompareLocationsUseCase,
) : VectorViewModel<LocationSharingViewState, LocationSharingAction, LocationSharingViewEvents>(initialState), LocationTracker.Callback {

    private val room = session.getRoom(initialState.roomId)!!

    private val locationTargetFlow = MutableSharedFlow<LocationData>()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationSharingViewModel, LocationSharingViewState> {
        override fun create(initialState: LocationSharingViewState): LocationSharingViewModel
    }

    companion object : MavericksViewModelFactory<LocationSharingViewModel, LocationSharingViewState> by hiltMavericksViewModelFactory()

    init {
        locationTracker.addCallback(this)
        locationTracker.start()
        setUserItem()
        updatePin()
        compareTargetAndUserLocation()
    }

    private fun setUserItem() {
        setState { copy(userItem = session.getUser(session.myUserId)?.toMatrixItem()) }
    }

    private fun updatePin(isUserPin: Boolean? = true) {
        if (isUserPin.orFalse()) {
            locationPinProvider.create(userId = session.myUserId) {
                updatePinDrawableInState(it)
            }
        } else {
            locationPinProvider.create(userId = null) {
                updatePinDrawableInState(it)
            }
        }
    }

    private fun updatePinDrawableInState(drawable: Drawable) {
        setState {
            copy(
                    locationTargetDrawable = drawable
            )
        }
    }

    private fun compareTargetAndUserLocation() {
        locationTargetFlow
                .sample(TARGET_LOCATION_CHANGE_SAMPLING_PERIOD_IN_MS)
                .map { compareTargetLocation(it) }
                .distinctUntilChanged()
                .onEach { setState { copy(areTargetAndUserLocationEqual = it) } }
                .onEach { updatePin(isUserPin = it) }
                .launchIn(viewModelScope)
    }

    private suspend fun compareTargetLocation(targetLocation: LocationData): Boolean? {
        return awaitState().lastKnownUserLocation
                ?.let { userLocation -> compareLocationsUseCase.execute(userLocation, targetLocation) }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.removeCallback(this)
    }

    override fun handle(action: LocationSharingAction) {
        when (action) {
            LocationSharingAction.CurrentUserLocationSharing  -> handleCurrentUserLocationSharingAction()
            is LocationSharingAction.PinnedLocationSharing    -> handlePinnedLocationSharingAction(action)
            is LocationSharingAction.LocationTargetChange     -> handleLocationTargetChangeAction(action)
            LocationSharingAction.ZoomToUserLocation          -> handleZoomToUserLocationAction()
            is LocationSharingAction.StartLiveLocationSharing -> handleStartLiveLocationSharingAction(action.durationMillis)
        }
    }

    private fun handleCurrentUserLocationSharingAction() = withState { state ->
        shareLocation(state.lastKnownUserLocation, isUserLocation = true)
    }

    private fun handlePinnedLocationSharingAction(action: LocationSharingAction.PinnedLocationSharing) {
        shareLocation(action.locationData, isUserLocation = false)
    }

    private fun shareLocation(locationData: LocationData?, isUserLocation: Boolean) {
        locationData?.let { location ->
            room.sendLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    uncertainty = location.uncertainty,
                    isUserLocation = isUserLocation
            )
            _viewEvents.post(LocationSharingViewEvents.Close)
        } ?: run {
            _viewEvents.post(LocationSharingViewEvents.LocationNotAvailableError)
        }
    }

    private fun handleLocationTargetChangeAction(action: LocationSharingAction.LocationTargetChange) {
        viewModelScope.launch {
            locationTargetFlow.emit(action.locationData)
        }
    }

    private fun handleZoomToUserLocationAction() = withState { state ->
        state.lastKnownUserLocation?.let { location ->
            _viewEvents.post(LocationSharingViewEvents.ZoomToUserLocation(location))
        }
    }

    private fun handleStartLiveLocationSharingAction(durationMillis: Long) {
        _viewEvents.post(LocationSharingViewEvents.StartLiveLocationService(
                sessionId = session.sessionId,
                roomId = room.roomId,
                durationMillis = durationMillis
        ))
    }

    override fun onLocationUpdate(locationData: LocationData) {
        setState {
            copy(lastKnownUserLocation = locationData)
        }
        viewModelScope.launch {
            // recompute location comparison using last received target location
            locationTargetFlow.lastOrNull()?.let {
                locationTargetFlow.emit(it)
            }
        }
    }

    override fun onLocationProviderIsNotAvailable() {
        _viewEvents.post(LocationSharingViewEvents.LocationNotAvailableError)
    }
}
