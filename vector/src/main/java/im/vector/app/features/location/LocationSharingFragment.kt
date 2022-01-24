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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLocationSharingBinding
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class LocationSharingFragment @Inject constructor(
        private val locationTracker: LocationTracker,
        private val session: Session,
        private val locationPinProvider: LocationPinProvider
) : VectorBaseFragment<FragmentLocationSharingBinding>(), LocationTracker.Callback {

    init {
        locationTracker.callback = this
    }

    private val viewModel: LocationSharingViewModel by fragmentViewModel()

    private var lastZoomValue: Double = -1.0

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationSharingBinding {
        return FragmentLocationSharingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.mapView.initialize {
            if (isAdded) {
                onMapReady()
            }
        }

        views.shareLocationContainer.debouncedClicks {
            viewModel.handle(LocationSharingAction.OnShareLocation)
        }

        viewModel.observeViewEvents {
            when (it) {
                LocationSharingViewEvents.LocationNotAvailableError    -> handleLocationNotAvailableError()
                LocationSharingViewEvents.Close                        -> activity?.finish()
            }.exhaustive
        }
    }

    override fun onPause() {
        views.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        views.mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        locationTracker.stop()
        super.onDestroy()
    }

    private fun onMapReady() {
        if (!isAdded) return

        locationPinProvider.create(session.myUserId) {
            views.mapView.addPinToMap(
                    pinId = USER_PIN_NAME,
                    image = it,
            )
            // All set, start location tracker
            locationTracker.start()
        }
    }

    override fun onLocationUpdate(locationData: LocationData) {
        lastZoomValue = if (lastZoomValue == -1.0) INITIAL_MAP_ZOOM else views.mapView.getCurrentZoom() ?: INITIAL_MAP_ZOOM

        views.mapView.zoomToLocation(locationData.latitude, locationData.longitude, lastZoomValue)
        views.mapView.deleteAllPins()
        views.mapView.updatePinLocation(USER_PIN_NAME, locationData.latitude, locationData.longitude)

        viewModel.handle(LocationSharingAction.OnLocationUpdate(locationData))
    }

    override fun onLocationProviderIsNotAvailable() {
        viewModel.handle(LocationSharingAction.OnLocationProviderIsNotAvailable)
    }

    private fun handleLocationNotAvailableError() {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.location_not_available_dialog_title)
                .setMessage(R.string.location_not_available_dialog_content)
                .setPositiveButton(R.string.ok) { _, _ ->
                    activity?.finish()
                }
                .show()
    }

    companion object {
        const val USER_PIN_NAME = "USER_PIN_NAME"
    }
}
