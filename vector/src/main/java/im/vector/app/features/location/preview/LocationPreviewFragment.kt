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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.mapbox.mapboxsdk.maps.MapView
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.openLocation
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentLocationPreviewBinding
import im.vector.app.features.location.DEFAULT_PIN_ID
import im.vector.app.features.location.LocationSharingArgs
import im.vector.app.features.location.MapState
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.showUserLocationNotAvailableErrorDialog
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Screen displaying the expanded map of a static location share.
 */
@AndroidEntryPoint
class LocationPreviewFragment :
        VectorBaseFragment<FragmentLocationPreviewBinding>(),
        VectorMenuProvider {

    @Inject lateinit var urlMapProvider: UrlMapProvider

    private val args: LocationSharingArgs by args()

    private val viewModel: LocationPreviewViewModel by fragmentViewModel()

    // Keep a ref to handle properly the onDestroy callback
    private var mapView: WeakReference<MapView>? = null

    private var mapLoadingErrorListener: MapView.OnDidFailLoadingMapListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationPreviewBinding {
        return FragmentLocationPreviewBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = WeakReference(views.mapView)
        mapLoadingErrorListener = MapView.OnDidFailLoadingMapListener {
            viewModel.handle(LocationPreviewAction.ShowMapLoadingError)
        }.also { views.mapView.addOnDidFailLoadingMapListener(it) }
        views.mapView.onCreate(savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                views.mapView.initialize(urlMapProvider.getMapUrl())
            }
        }

        observeViewEvents()
        initLocateButton()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                LocationPreviewViewEvents.UserLocationNotAvailableError -> handleUserLocationNotAvailableError()
                is LocationPreviewViewEvents.ZoomToUserLocation -> handleZoomToUserLocationEvent(it)
            }
        }
    }

    private fun handleUserLocationNotAvailableError() {
        showUserLocationNotAvailableErrorDialog {
            // do nothing
        }
    }

    private fun handleZoomToUserLocationEvent(event: LocationPreviewViewEvents.ZoomToUserLocation) {
        views.mapView.zoomToLocation(event.userLocation)
    }

    override fun onDestroyView() {
        mapLoadingErrorListener?.let { mapView?.get()?.removeOnDidFailLoadingMapListener(it) }
        mapLoadingErrorListener = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        views.mapView.onResume()
    }

    override fun onPause() {
        views.mapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        views.mapView.onLowMemory()
        super.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        views.mapView.onStart()
    }

    override fun onStop() {
        views.mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        views.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mapView?.get()?.onDestroy()
        mapView?.clear()
        super.onDestroy()
    }

    override fun invalidate() = withState(viewModel) { state ->
        views.mapPreviewLoadingError.isVisible = state.loadingMapHasFailed
        if (state.isLoadingUserLocation) {
            showLoadingDialog()
        } else {
            dismissLoadingDialog()
        }
        updateMap(state)
    }

    private fun updateMap(viewState: LocationPreviewViewState) {
        views.mapView.render(
                MapState(
                        zoomOnlyOnce = true,
                        pinLocationData = viewState.pinLocationData,
                        pinId = viewState.pinUserId ?: DEFAULT_PIN_ID,
                        pinDrawable = viewState.pinDrawable,
                        userLocationData = viewState.lastKnownUserLocation,
                )
        )
    }

    override fun getMenuRes() = R.menu.menu_location_preview

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_external -> {
                onShareLocationExternal()
                true
            }
            else -> false
        }
    }

    private fun onShareLocationExternal() {
        val location = args.initialLocationData ?: return
        openLocation(requireActivity(), location.latitude, location.longitude)
    }

    private fun initLocateButton() {
        views.mapView.locateButton.setOnClickListener {
            if (checkPermissions(PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING, requireActivity(), foregroundLocationResultLauncher)) {
                zoomToUserLocation()
            }
        }
    }

    private fun zoomToUserLocation() {
        viewModel.handle(LocationPreviewAction.ZoomToUserLocation)
    }

    private val foregroundLocationResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            zoomToUserLocation()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(R.string.denied_permission_generic)
        }
    }
}
