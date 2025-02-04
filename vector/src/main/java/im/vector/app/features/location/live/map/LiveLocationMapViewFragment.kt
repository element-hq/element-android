/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
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
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.core.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.openLocation
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentLiveLocationMapViewBinding
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.showUserLocationNotAvailableErrorDialog
import im.vector.app.features.location.zoomToBounds
import im.vector.app.features.location.zoomToLocation
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val USER_LOCATION_PIN_ID = "user-location-pin-id"

/**
 * Screen showing a map with all the current users sharing their live location in a room.
 */
@AndroidEntryPoint
class LiveLocationMapViewFragment :
        VectorBaseFragment<FragmentLiveLocationMapViewBinding>() {

    @Inject lateinit var urlMapProvider: UrlMapProvider
    @Inject lateinit var bottomSheetController: LiveLocationBottomSheetController
    @Inject lateinit var dimensionConverter: DimensionConverter
    @Inject lateinit var drawableProvider: DrawableProvider

    private val viewModel: LiveLocationMapViewModel by fragmentViewModel()

    private var mapboxMap: WeakReference<MapboxMap>? = null
    private var mapView: MapView? = null
    private var symbolManager: SymbolManager? = null
    private var mapStyle: Style? = null
    private val userLocationDrawable by lazy { drawableProvider.getDrawable(R.drawable.ic_location_user) }
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
        initLocateButton()

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
                is LiveLocationMapViewEvents.LiveLocationError -> displayErrorDialog(viewEvent.error)
                is LiveLocationMapViewEvents.ZoomToUserLocation -> handleZoomToUserLocationEvent(viewEvent)
                LiveLocationMapViewEvents.UserLocationNotAvailableError -> handleUserLocationNotAvailableError()
            }
        }
    }

    private fun handleZoomToUserLocationEvent(event: LiveLocationMapViewEvents.ZoomToUserLocation) {
        mapboxMap?.get().zoomToLocation(event.userLocation)
    }

    private fun handleUserLocationNotAvailableError() {
        showUserLocationNotAvailableErrorDialog {
            // do nothing
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
                    // force refresh of the map using the last viewState
                    invalidate()
                }
            }
        }
    }

    private fun initLocateButton() {
        views.liveLocationMapLocateButton.setOnClickListener {
            if (checkPermissions(PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING, requireActivity(), foregroundLocationResultLauncher)) {
                zoomToUserLocation()
            }
        }
    }

    private fun zoomToUserLocation() {
        viewModel.handle(LiveLocationMapAction.ZoomToUserLocation)
    }

    private val foregroundLocationResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            zoomToUserLocation()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(CommonStrings.denied_permission_generic)
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
            updateMap(userLiveLocations = viewState.userLocations, userLocation = viewState.lastKnownUserLocation)
        }
        if (viewState.isLoadingUserLocation) {
            showLoadingDialog()
        } else {
            dismissLoadingDialog()
        }
        updateUserListBottomSheet(viewState.userLocations)
        updateLocateButton(showLocateButton = viewState.showLocateUserButton)
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

    private fun updateLocateButton(showLocateButton: Boolean) {
        views.liveLocationMapLocateButton.isVisible = showLocateButton
        adjustCompassButton()
    }

    private fun adjustCompassButton() {
        val locateButton = views.liveLocationMapLocateButton
        locateButton.post {
            val marginTop = locateButton.height + locateButton.marginTop + locateButton.marginBottom
            val marginRight =
                    locateButton.context.resources.getDimensionPixelOffset(im.vector.lib.ui.styles.R.dimen.location_sharing_compass_button_margin_horizontal)
            mapboxMap?.get()?.uiSettings?.setCompassMargins(0, marginTop, marginRight, 0)
        }
    }

    private fun updateMap(
            userLiveLocations: List<UserLiveLocationViewState>,
            userLocation: LocationData?,
    ) {
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
            if (userLocation == null) {
                removeUserSymbol(sManager)
            } else {
                createOrUpdateUserSymbol(userLocation, sManager)
            }
        }
    }

    private fun createOrUpdateSymbol(userLocation: UserLiveLocationViewState, symbolManager: SymbolManager) {
        val pinId = userLocation.matrixItem.id
        val pinDrawable = userLocation.pinDrawable
        createOrUpdateSymbol(pinId, pinDrawable, userLocation.locationData, symbolManager)
    }

    private fun createOrUpdateUserSymbol(locationData: LocationData, symbolManager: SymbolManager) {
        userLocationDrawable?.let { pinDrawable -> createOrUpdateSymbol(USER_LOCATION_PIN_ID, pinDrawable, locationData, symbolManager) }
    }

    private fun removeUserSymbol(symbolManager: SymbolManager) = withState(viewModel) { state ->
        val pinId = USER_LOCATION_PIN_ID
        state.mapSymbolIds[pinId]?.let { symbolId ->
            removeSymbol(pinId, symbolId, symbolManager)
        }
    }

    private fun createOrUpdateSymbol(
            pinId: String,
            pinDrawable: Drawable,
            locationData: LocationData,
            symbolManager: SymbolManager
    ) = withState(viewModel) { state ->
        val symbolId = state.mapSymbolIds[pinId]

        if (symbolId == null || symbolManager.annotations.get(symbolId) == null) {
            createSymbol(pinId, pinDrawable, locationData, symbolManager)
        } else {
            updateSymbol(symbolId, locationData, symbolManager)
        }
    }

    private fun createSymbol(
            pinId: String,
            pinDrawable: Drawable,
            locationData: LocationData,
            symbolManager: SymbolManager
    ) {
        addPinToMapStyle(pinId, pinDrawable)
        val symbolOptions = buildSymbolOptions(locationData, pinId)
        val symbol = symbolManager.create(symbolOptions)
        viewModel.handle(LiveLocationMapAction.AddMapSymbol(pinId, symbol.id))
    }

    private fun updateSymbol(symbolId: Long, locationData: LocationData, symbolManager: SymbolManager) {
        val newLocation = LatLng(locationData.latitude, locationData.longitude)
        val symbol = symbolManager.annotations.get(symbolId)
        symbol?.let {
            it.latLng = newLocation
            symbolManager.update(it)
        }
    }

    private fun removeOutdatedSymbols(userLiveLocations: List<UserLiveLocationViewState>, symbolManager: SymbolManager) = withState(viewModel) { state ->
        val pinIdsToKeep = userLiveLocations.map { it.matrixItem.id } + USER_LOCATION_PIN_ID
        val pinIdsToRemove = state.mapSymbolIds.keys.subtract(pinIdsToKeep.toSet())
        pinIdsToRemove.forEach { pinId ->
            val symbolId = state.mapSymbolIds[pinId]
            removeSymbol(pinId, symbolId, symbolManager)
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

    private fun addPinToMapStyle(pinId: String, pinDrawable: Drawable) {
        mapStyle?.let { style ->
            if (style.getImage(pinId) == null) {
                style.addImage(pinId, pinDrawable.toBitmap())
            }
        }
    }

    private fun removeSymbol(pinId: String, symbolId: Long?, symbolManager: SymbolManager) {
        removeUserPinFromMapStyle(pinId)

        symbolId?.let { id ->
            Timber.d("trying to delete symbol with id: $id")
            symbolManager.annotations.get(id)?.let {
                symbolManager.delete(it)
            }
        }

        viewModel.handle(LiveLocationMapAction.RemoveMapSymbol(pinId))
    }

    private fun removeUserPinFromMapStyle(pinId: String) {
        mapStyle?.removeImage(pinId)
    }

    private fun buildSymbolOptions(locationData: LocationData, pinId: String) =
            SymbolOptions()
                    .withLatLng(LatLng(locationData.latitude, locationData.longitude))
                    .withIconImage(pinId)
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
