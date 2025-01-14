/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.settings.VectorPreferences
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
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber

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
        private val vectorPreferences: VectorPreferences,
) : VectorViewModel<LocationSharingViewState, LocationSharingAction, LocationSharingViewEvents>(initialState), LocationTracker.Callback {

    private val room = session.getRoom(initialState.roomId)!!

    private val locationTargetFlow = MutableSharedFlow<LocationData>()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationSharingViewModel, LocationSharingViewState> {
        override fun create(initialState: LocationSharingViewState): LocationSharingViewModel
    }

    companion object : MavericksViewModelFactory<LocationSharingViewModel, LocationSharingViewState> by hiltMavericksViewModelFactory()

    init {
        initLocationTracking()
        setUserItem()
        updatePin()
        compareTargetAndUserLocation()
        observePowerLevelsForLiveLocationSharing()
    }

    private fun observePowerLevelsForLiveLocationSharing() {
        PowerLevelsFlowFactory(room).createFlow()
                .distinctUntilChanged()
                .setOnEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val canShareLiveLocation = EventType.STATE_ROOM_BEACON_INFO.values
                            .all { beaconInfoType ->
                                powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, beaconInfoType)
                            }

                    copy(canShareLiveLocation = canShareLiveLocation)
                }
    }

    private fun initLocationTracking() {
        locationTracker.addCallback(this)
        locationTracker.locations
                .onEach(::onLocationUpdate)
                .launchIn(viewModelScope)
        locationTracker.start()
    }

    private fun setUserItem() {
        setState { copy(userItem = session.getUserOrDefault(session.myUserId).toMatrixItem()) }
    }

    private fun updatePin(isUserPin: Boolean? = true) {
        if (isUserPin.orFalse()) {
            val matrixItem = room.membershipService().getRoomMember(session.myUserId)?.toMatrixItem()
                    ?: session.getUserOrDefault(session.myUserId).toMatrixItem()
            locationPinProvider.create(matrixItem) {
                updatePinDrawableInState(it)
            }
        } else {
            locationPinProvider.create(null) {
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
            LocationSharingAction.CurrentUserLocationSharing -> handleCurrentUserLocationSharingAction()
            is LocationSharingAction.PinnedLocationSharing -> handlePinnedLocationSharingAction(action)
            is LocationSharingAction.LocationTargetChange -> handleLocationTargetChangeAction(action)
            LocationSharingAction.ZoomToUserLocation -> handleZoomToUserLocationAction()
            LocationSharingAction.LiveLocationSharingRequested -> handleLiveLocationSharingRequestedAction()
            is LocationSharingAction.StartLiveLocationSharing -> handleStartLiveLocationSharingAction(action.durationMillis)
            LocationSharingAction.ShowMapLoadingError -> handleShowMapLoadingError()
        }
    }

    private fun handleLiveLocationSharingRequestedAction() = withState { state ->
        if (!state.canShareLiveLocation) {
            _viewEvents.post(LocationSharingViewEvents.LiveLocationSharingNotEnoughPermission)
        } else if (vectorPreferences.labsEnableLiveLocation()) {
            _viewEvents.post(LocationSharingViewEvents.ChooseLiveLocationDuration)
        } else {
            _viewEvents.post(LocationSharingViewEvents.ShowLabsFlagPromotion)
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
            viewModelScope.launch {
                room.locationSharingService().sendStaticLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        uncertainty = location.uncertainty,
                        isUserLocation = isUserLocation
                )
                _viewEvents.post(LocationSharingViewEvents.Close)
            }
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
        _viewEvents.post(
                LocationSharingViewEvents.StartLiveLocationService(
                        sessionId = session.sessionId,
                        roomId = room.roomId,
                        durationMillis = durationMillis
                )
        )
    }

    private fun handleShowMapLoadingError() {
        setState { copy(loadingMapHasFailed = true) }
    }

    private fun onLocationUpdate(locationData: LocationData) {
        Timber.d("onLocationUpdate()")
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

    override fun onNoLocationProviderAvailable() {
        _viewEvents.post(LocationSharingViewEvents.LocationNotAvailableError)
    }
}
