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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import dagger.hilt.android.AndroidEntryPoint
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import im.vector.app.R
import im.vector.app.core.extensions.addChildFragment
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSimpleContainerBinding
import im.vector.app.features.location.UrlMapProvider
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Screen showing a map with all the current users sharing their live location in room.
 */
@AndroidEntryPoint
class LocationLiveMapViewFragment : VectorBaseFragment<FragmentSimpleContainerBinding>() {

    @Inject
    lateinit var urlMapProvider: UrlMapProvider

    private val args: LocationLiveMapViewArgs by args()

    private val viewModel: LocationLiveMapViewModel by fragmentViewModel()

    private var mapboxMap: WeakReference<MapboxMap>? = null
    private var symbolManager: SymbolManager? = null
    private var mapStyle: Style? = null
    private val pendingLiveLocations = mutableListOf<UserLiveLocationViewState>()
    private var isMapFirstUpdate = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSimpleContainerBinding {
        return FragmentSimpleContainerBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
    }

    private fun setupMap() {
        val mapFragment = getOrCreateSupportMapFragment()

        mapFragment.getMapAsync { mapboxMap ->
            lifecycleScope.launchWhenCreated {
                mapboxMap.setStyle(urlMapProvider.getMapUrl()) { style ->
                    mapStyle = style
                    this@LocationLiveMapViewFragment.mapboxMap = WeakReference(mapboxMap)
                    symbolManager = SymbolManager(mapFragment.view as MapView, mapboxMap, style)
                    pendingLiveLocations
                            .takeUnless { it.isEmpty() }
                            ?.let { updateMap(it) }
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        updateMap(viewState.userLocations)
    }

    private fun updateMap(userLiveLocations: List<UserLiveLocationViewState>) {
        symbolManager?.let {
            it.deleteAll()

            val latLngBoundsBuilder = LatLngBounds.Builder()
            userLiveLocations.forEach { userLocation ->
                addUserPinToMapStyle(userLocation.userId, userLocation.pinDrawable)
                val symbolOptions = buildSymbolOptions(userLocation)
                it.create(symbolOptions)

                if (isMapFirstUpdate) {
                    latLngBoundsBuilder.include(LatLng(userLocation.locationData.latitude, userLocation.locationData.longitude))
                }
            }

            if (isMapFirstUpdate) {
                isMapFirstUpdate = false
                zoomToViewAllUsers(latLngBoundsBuilder.build())
            }
        } ?: run {
            pendingLiveLocations.clear()
            pendingLiveLocations.addAll(userLiveLocations)
        }
    }

    private fun addUserPinToMapStyle(userId: String, userPinDrawable: Drawable) {
        mapStyle?.let { style ->
            if (style.getImage(userId) == null) {
                style.addImage(userId, userPinDrawable)
            }
        }
    }

    private fun buildSymbolOptions(userLiveLocation: UserLiveLocationViewState) =
            SymbolOptions()
                    .withLatLng(LatLng(userLiveLocation.locationData.latitude, userLiveLocation.locationData.longitude))
                    .withIconImage(userLiveLocation.userId)
                    .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)

    private fun zoomToViewAllUsers(latLngBounds: LatLngBounds) {
        mapboxMap?.get()?.let { mapboxMap ->
            mapboxMap.getCameraForLatLngBounds(latLngBounds)?.let { cameraPosition ->
                // update the zoom a little to avoid having pins exactly at the edges of the map
                mapboxMap.cameraPosition = CameraPosition.Builder(cameraPosition)
                        .zoom((cameraPosition.zoom - 1).coerceAtLeast(MapboxConstants.MINIMUM_ZOOM.toDouble()))
                        .build()
            }
        }
    }

    private fun getOrCreateSupportMapFragment() =
            childFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment
                    ?: run {
                        val options = MapboxMapOptions.createFromAttributes(requireContext(), null)
                        SupportMapFragment.newInstance(options)
                                .also { addChildFragment(R.id.fragmentContainer, it, tag = MAP_FRAGMENT_TAG) }
                    }

    companion object {
        private const val MAP_FRAGMENT_TAG = "im.vector.app.features.location.live.map"
    }
}
