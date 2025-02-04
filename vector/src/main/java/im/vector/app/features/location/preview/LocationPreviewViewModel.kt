/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

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
        val matrixItem = if (initialState.roomId != null && initialState.pinUserId != null) {
            session
                    .roomService()
                    .getRoom(initialState.roomId)
                    ?.membershipService()
                    ?.getRoomMember(initialState.pinUserId)
                    ?.toMatrixItem()
                    ?: MatrixItem.UserItem(initialState.pinUserId)
        } else {
            null
        }
        initPin(matrixItem)
        initLocationTracking()
    }

    private fun initPin(matrixItem: MatrixItem?) {
        locationPinProvider.create(matrixItem) { pinDrawable ->
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
