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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addChildFragment
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.core.utils.openLocation
import im.vector.app.databinding.FragmentLiveLocationMapViewBinding
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.zoomToBounds
import im.vector.app.features.location.zoomToLocation
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Screen showing a map with all the current users sharing their live location in a room.
 */
@AndroidEntryPoint
class LiveLocationMapViewFragment :
        VectorBaseFragment<FragmentLiveLocationMapViewBinding>() {

    @Inject lateinit var urlMapProvider: UrlMapProvider
    @Inject lateinit var bottomSheetController: LiveLocationBottomSheetController
    @Inject lateinit var dimensionConverter: DimensionConverter

    private val viewModel: LiveLocationMapViewModel by fragmentViewModel()

    private var mapboxMap: WeakReference<MapboxMap>? = null
    private var mapView: MapView? = null
    private var symbolManager: SymbolManager? = null
    private var mapStyle: Style? = null
    private val pendingLiveLocations = mutableListOf<UserLiveLocationViewState>()
    private var isMapFirstUpdate = true
    private var onSymbolClickListener: OnSymbolClickListener? = null
    private var mapLoadingErrorListener: MapView.OnDidFailLoadingMapListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLiveLocationMapViewBinding {
        return FragmentLiveLocationMapViewBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        setupMap()

        views.liveLocationBottomSheetRecyclerView.configureWith(bottomSheetController, hasFixedSize = false, disableItemAnimation = true)

        bottomSheetController.callback = object : LiveLocationBottomSheetController.Callback {
            override fun onUserSelected(userId: String) {
                handleBottomSheetUserSelected(userId)
            }

            override fun onStopLocationClicked() {
                viewModel.handle(LiveLocationMapAction.StopSharing)
            }
        }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents { viewEvent ->
            when (viewEvent) {
                is LiveLocationMapViewEvents.Error -> displayErrorDialog(viewEvent.error)
            }
        }
    }

    override fun onDestroyView() {
        onSymbolClickListener?.let { symbolManager?.removeClickListener(it) }
        symbolManager?.onDestroy()
        bottomSheetController.callback = null
        views.liveLocationBottomSheetRecyclerView.cleanup()
        mapLoadingErrorListener?.let { mapView?.removeOnDidFailLoadingMapListener(it) }
        mapLoadingErrorListener = null
        mapView = null
        super.onDestroyView()
    }

    private fun setupMap() {
        val mapFragment = getOrCreateSupportMapFragment()
        mapFragment.getMapAsync { mapboxMap ->
            (mapFragment.view as? MapView)?.let {
                mapView = it
                listenMapLoadingError(it)
            }
            lifecycleScope.launch {
                mapboxMap.setStyle(urlMapProvider.getMapUrl()) { style ->
                    mapStyle = style
                    this@LiveLocationMapViewFragment.mapboxMap = WeakReference(mapboxMap)
                    symbolManager = SymbolManager(mapFragment.view as MapView, mapboxMap, style).apply {
                        iconAllowOverlap = true
                        onSymbolClickListener = OnSymbolClickListener {
                            onSymbolClicked(it)
                            true
                        }.also { addClickListener(it) }
                    }
                    pendingLiveLocations
                            .takeUnless { it.isEmpty() }
                            ?.let { updateMap(it) }
                }
            }
        }
    }

    private fun listenMapLoadingError(mapView: MapView) {
        mapLoadingErrorListener = MapView.OnDidFailLoadingMapListener {
            viewModel.handle(LiveLocationMapAction.ShowMapLoadingError)
        }.also { mapView.addOnDidFailLoadingMapListener(it) }
    }

    private fun onSymbolClicked(symbol: Symbol?) {
        symbol?.let {
            mapboxMap
                    ?.get()
                    ?.zoomToLocation(LocationData(it.latLng.latitude, it.latLng.longitude, null), preserveCurrentZoomLevel = false)

            LiveLocationMapMarkerOptionsDialog(requireContext())
                    .apply {
                        callback = object : LiveLocationMapMarkerOptionsDialog.Callback {
                            override fun onShareLocationClicked() {
                                shareLocation(symbol)
                                dismiss()
                            }
                        }
                    }
                    .show(views.liveLocationPopupAnchor)
        }
    }

    private fun shareLocation(symbol: Symbol) {
        openLocation(requireActivity(), symbol.latLng.latitude, symbol.latLng.longitude)
    }

    private fun getOrCreateSupportMapFragment() =
            childFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment
                    ?: run {
                        val options = MapboxMapOptions.createFromAttributes(requireContext(), null)
                        SupportMapFragment.newInstance(options)
                                .also { addChildFragment(R.id.liveLocationMapFragmentContainer, it, tag = MAP_FRAGMENT_TAG) }
                    }

    override fun invalidate() = withState(viewModel) { viewState ->
        if (viewState.loadingMapHasFailed) {
            views.mapPreviewLoadingError.isVisible = true
        } else {
            views.mapPreviewLoadingError.isGone = true
            updateMap(viewState.userLocations)
        }
        updateUserListBottomSheet(viewState.userLocations)
    }

    private fun updateUserListBottomSheet(userLocations: List<UserLiveLocationViewState>) {
        if (userLocations.isEmpty()) {
            showEndedLiveBanner()
        } else {
            showUserList(userLocations)
        }
    }

    private fun showEndedLiveBanner() {
        views.bottomSheet.isGone = true
        views.liveLocationMapFragmentEndedBanner.isVisible = true
        updateCopyrightMargin(bottomOffset = views.liveLocationMapFragmentEndedBanner.height)
    }

    private fun showUserList(userLocations: List<UserLiveLocationViewState>) {
        val bottomSheetHeight = BottomSheetBehavior.from(views.bottomSheet).peekHeight
        updateCopyrightMargin(bottomOffset = bottomSheetHeight)
        views.bottomSheet.isVisible = true
        views.liveLocationMapFragmentEndedBanner.isGone = true
        bottomSheetController.setData(userLocations)
    }

    private fun updateCopyrightMargin(bottomOffset: Int) {
        getOrCreateSupportMapFragment().getMapAsync { mapboxMap ->
            mapboxMap.uiSettings.apply {
                // Place copyright above the user list bottom sheet
                setLogoMargins(
                        dimensionConverter.dpToPx(COPYRIGHT_MARGIN_DP),
                        0,
                        0,
                        bottomOffset + dimensionConverter.dpToPx(COPYRIGHT_MARGIN_DP)
                )
                setAttributionMargins(
                        dimensionConverter.dpToPx(COPYRIGHT_ATTRIBUTION_MARGIN_DP),
                        0,
                        0,
                        bottomOffset + dimensionConverter.dpToPx(COPYRIGHT_MARGIN_DP)
                )
            }
        }
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

    private fun createOrUpdateSymbol(userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) = withState(viewModel) { state ->
        val symbolId = state.mapSymbolIds[userLocation.matrixItem.id]

        if (symbolId == null || symbolManager.annotations.get(symbolId) == null) {
            createSymbol(userLocation, symbolManager)
        } else {
            updateSymbol(symbolId, userLocation, symbolManager)
        }
    }

    private fun createSymbol(userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        addUserPinToMapStyle(userLocation.matrixItem.id, userLocation.pinDrawable)
        val symbolOptions = buildSymbolOptions(userLocation)
        val symbol = symbolManager.create(symbolOptions)
        viewModel.handle(LiveLocationMapAction.AddMapSymbol(userLocation.matrixItem.id, symbol.id))
    }

    private fun updateSymbol(symbolId: Long, userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        val newLocation = LatLng(userLocation.locationData.latitude, userLocation.locationData.longitude)
        val symbol = symbolManager.annotations.get(symbolId)
        symbol?.let {
            it.latLng = newLocation
            symbolManager.update(it)
        }
    }

    private fun removeOutdatedSymbols(userLiveLocations: List<UserLiveLocationViewState>, symbolManager: SymbolManager) = withState(viewModel) { state ->
        val userIdsToRemove = state.mapSymbolIds.keys.subtract(userLiveLocations.map { it.matrixItem.id }.toSet())
        userIdsToRemove.forEach { userId ->
            removeUserPinFromMapStyle(userId)
            viewModel.handle(LiveLocationMapAction.RemoveMapSymbol(userId))

            state.mapSymbolIds[userId]?.let { symbolId ->
                Timber.d("trying to delete symbol with id: $symbolId")
                symbolManager.annotations.get(symbolId)?.let {
                    symbolManager.delete(it)
                }
            }
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
                style.addImage(userId, userPinDrawable.toBitmap())
            }
        }
    }

    private fun removeUserPinFromMapStyle(userId: String) {
        mapStyle?.removeImage(userId)
    }

    private fun buildSymbolOptions(userLiveLocation: UserLiveLocationViewState) =
            SymbolOptions()
                    .withLatLng(LatLng(userLiveLocation.locationData.latitude, userLiveLocation.locationData.longitude))
                    .withIconImage(userLiveLocation.matrixItem.id)
                    .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)

    private fun handleBottomSheetUserSelected(userId: String) = withState(viewModel) { state ->
        state.userLocations
                .find { it.matrixItem.id == userId }
                ?.locationData
                ?.let { locationData ->
                    mapboxMap?.get()?.zoomToLocation(locationData, preserveCurrentZoomLevel = false)
                }
    }

    companion object {
        private const val MAP_FRAGMENT_TAG = "im.vector.app.features.location.live.map"
        private const val COPYRIGHT_MARGIN_DP = 8
        private const val COPYRIGHT_ATTRIBUTION_MARGIN_DP = 96
    }
}
