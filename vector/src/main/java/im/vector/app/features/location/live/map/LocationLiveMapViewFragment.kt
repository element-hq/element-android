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
import im.vector.app.features.location.zoomToBounds
import im.vector.app.features.location.zoomToLocation
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Screen showing a map with all the current users sharing their live location in a room.
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

    private fun getOrCreateSupportMapFragment() =
            childFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment
                    ?: run {
                        val options = MapboxMapOptions.createFromAttributes(requireContext(), null)
                        SupportMapFragment.newInstance(options)
                                .also { addChildFragment(R.id.fragmentContainer, it, tag = MAP_FRAGMENT_TAG) }
                    }

    override fun invalidate() = withState(viewModel) { viewState ->
        updateMap(viewState.userLocations)
    }

    private fun updateMap(userLiveLocations: List<UserLiveLocationViewState>) {
        symbolManager?.let { sManager ->
            val latLngBoundsBuilder = LatLngBounds.Builder()

            userLiveLocations.forEach { userLocation ->
                createOrUpdateSymbol(userLocation, sManager)

                if (isMapFirstUpdate) {
                    val latLng = LatLng(userLocation.locationData.latitude, userLocation.locationData.longitude)
                    latLngBoundsBuilder.include(latLng)
                }
            }

            removeOutdatedSymbols(userLiveLocations, sManager)
            updateMapZoomWhenNeeded(userLiveLocations, latLngBoundsBuilder)

        } ?: postponeUpdateOfMap(userLiveLocations)
    }

    private fun createOrUpdateSymbol(userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        val symbolId = viewModel.mapSymbolIds[userLocation.userId]

        if (symbolId == null) {
            createSymbol(userLocation, symbolManager)
        } else {
            updateSymbol(symbolId, userLocation, symbolManager)
        }
    }

    private fun createSymbol(userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        addUserPinToMapStyle(userLocation.userId, userLocation.pinDrawable)
        val symbolOptions = buildSymbolOptions(userLocation)
        val symbol = symbolManager.create(symbolOptions)
        viewModel.mapSymbolIds[userLocation.userId] = symbol.id
    }

    private fun updateSymbol(symbolId: Long, userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        val newLocation = LatLng(userLocation.locationData.latitude, userLocation.locationData.longitude)
        val symbol = symbolManager.annotations.get(symbolId)
        symbol?.let {
            it.latLng = newLocation
            symbolManager.update(it)
        }
    }

    private fun removeOutdatedSymbols(userLiveLocations: List<UserLiveLocationViewState>, symbolManager: SymbolManager) {
        val userIdsToRemove = viewModel.mapSymbolIds.keys.subtract(userLiveLocations.map { it.userId }.toSet())
        userIdsToRemove
                .mapNotNull { userId ->
                    removeUserPinFromMapStyle(userId)
                    viewModel.mapSymbolIds[userId]
                }
                .forEach { symbolId ->
                    val symbol = symbolManager.annotations.get(symbolId)
                    symbolManager.delete(symbol)
                }
    }

    private fun updateMapZoomWhenNeeded(userLiveLocations: List<UserLiveLocationViewState>, latLngBoundsBuilder: LatLngBounds.Builder) {
        if (userLiveLocations.isNotEmpty() && isMapFirstUpdate) {
            isMapFirstUpdate = false
            if (userLiveLocations.size > 1) {
                mapboxMap?.get()?.zoomToBounds(latLngBoundsBuilder.build())
            } else {
                mapboxMap?.get()?.zoomToLocation(userLiveLocations.first().locationData)
            }
        }
    }

    private fun postponeUpdateOfMap(userLiveLocations: List<UserLiveLocationViewState>) {
        pendingLiveLocations.clear()
        pendingLiveLocations.addAll(userLiveLocations)
    }

    private fun addUserPinToMapStyle(userId: String, userPinDrawable: Drawable) {
        mapStyle?.let { style ->
            if (style.getImage(userId) == null) {
                style.addImage(userId, userPinDrawable)
            }
        }
    }

    private fun removeUserPinFromMapStyle(userId: String) {
        mapStyle?.removeImage(userId)
    }

    private fun buildSymbolOptions(userLiveLocation: UserLiveLocationViewState) =
            SymbolOptions()
                    .withLatLng(LatLng(userLiveLocation.locationData.latitude, userLiveLocation.locationData.longitude))
                    .withIconImage(userLiveLocation.userId)
                    .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)

    companion object {
        private const val MAP_FRAGMENT_TAG = "im.vector.app.features.location.live.map"
    }
}
