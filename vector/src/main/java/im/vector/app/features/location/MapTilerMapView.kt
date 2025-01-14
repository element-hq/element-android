/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.lib.strings.CommonStrings
import timber.log.Timber

private const val USER_PIN_ID = "user-pin-id"

class MapTilerMapView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : MapView(context, attrs, defStyleAttr) {

    private var pendingState: MapState? = null

    data class MapRefs(
            val map: MapboxMap,
            val symbolManager: SymbolManager,
            val style: Style
    )

    private val userLocationDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_location_user)
    }
    val locateButton by lazy { createLocateButton() }
    private var mapRefs: MapRefs? = null
    private var initZoomDone = false
    private var showLocationButton = false
    private var dimensionConverter: DimensionConverter? = null

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.MapTilerMapView,
                0,
                0
        ).use {
            setLocateButtonVisibility(it)
        }
        dimensionConverter = DimensionConverter(resources)
    }

    private fun setLocateButtonVisibility(typedArray: TypedArray) {
        showLocationButton = typedArray.getBoolean(im.vector.lib.ui.styles.R.styleable.MapTilerMapView_showLocateButton, false)
    }

    override fun onDestroy() {
        mapRefs?.symbolManager?.onDestroy()
        mapRefs = null
        super.onDestroy()
    }

    /**
     * For location fragments.
     */
    fun initialize(
            url: String,
            locationTargetChangeListener: LocationTargetChangeListener? = null
    ) {
        Timber.d("## Location: initialize")
        getMapAsync { map ->
            initMapStyle(map, url)
            initLocateButton(map)
            notifyLocationOfMapCenter(locationTargetChangeListener)
            listenCameraMove(map, locationTargetChangeListener)
        }
    }

    private fun initMapStyle(map: MapboxMap, url: String) {
        map.setStyle(url) { style ->
            val symbolManager = SymbolManager(this, map, style)
            symbolManager.iconAllowOverlap = true
            mapRefs = MapRefs(
                    map,
                    symbolManager,
                    style
            )
            pendingState?.let { render(it) }
            pendingState = null
        }
    }

    private fun initLocateButton(map: MapboxMap) {
        if (showLocationButton) {
            addView(locateButton)
            adjustCompassButton(map)
        }
    }

    private fun createLocateButton(): ImageView =
            ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.btn_locate))
                contentDescription = context.getString(CommonStrings.a11y_location_share_locate_button)
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                updateLayoutParams<MarginLayoutParams> {
                    val marginHorizontal =
                            context.resources.getDimensionPixelOffset(im.vector.lib.ui.styles.R.dimen.location_sharing_locate_button_margin_horizontal)
                    val marginVertical =
                            context.resources.getDimensionPixelOffset(im.vector.lib.ui.styles.R.dimen.location_sharing_locate_button_margin_vertical)
                    setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
                }
                updateLayoutParams<LayoutParams> {
                    gravity = Gravity.TOP or Gravity.END
                }
            }

    private fun adjustCompassButton(map: MapboxMap) {
        locateButton.post {
            val marginTop = locateButton.height + locateButton.marginTop + locateButton.marginBottom
            val marginRight = context.resources.getDimensionPixelOffset(im.vector.lib.ui.styles.R.dimen.location_sharing_compass_button_margin_horizontal)
            map.uiSettings.setCompassMargins(0, marginTop, marginRight, 0)
        }
    }

    private fun listenCameraMove(map: MapboxMap, locationTargetChangeListener: LocationTargetChangeListener?) {
        map.addOnCameraMoveListener {
            notifyLocationOfMapCenter(locationTargetChangeListener)
        }
    }

    private fun notifyLocationOfMapCenter(locationTargetChangeListener: LocationTargetChangeListener?) {
        getLocationOfMapCenter()?.let { target ->
            locationTargetChangeListener?.onLocationTargetChange(target)
        }
    }

    fun render(state: MapState) {
        val safeMapRefs = mapRefs ?: return Unit.also {
            pendingState = state
        }

        safeMapRefs.map.uiSettings.apply {
            setLogoMargins(0, 0, 0, state.logoMarginBottom)
            dimensionConverter?.let {
                setAttributionMargins(it.dpToPx(88), 0, 0, state.logoMarginBottom)
            }
        }

        val pinDrawable = state.pinDrawable ?: userLocationDrawable
        addImageToMapStyle(pinDrawable, state.pinId, safeMapRefs)

        safeMapRefs.symbolManager.deleteAll()
        state.pinLocationData?.let { locationData ->
            if (!initZoomDone || !state.zoomOnlyOnce) {
                zoomToLocation(locationData)
                initZoomDone = true
            }

            if (pinDrawable != null && state.showPin) {
                createSymbol(locationData, state.pinId, safeMapRefs)
            }
        }

        state.userLocationData?.let { locationData ->
            addImageToMapStyle(userLocationDrawable, USER_PIN_ID, safeMapRefs)
            if (userLocationDrawable != null) {
                createSymbol(locationData, USER_PIN_ID, safeMapRefs)
            }
        }
    }

    private fun addImageToMapStyle(image: Drawable?, imageId: String, mapRefs: MapRefs) {
        image?.let { drawable ->
            if (!mapRefs.style.isFullyLoaded || mapRefs.style.getImage(imageId) == null) {
                mapRefs.style.addImage(imageId, drawable.toBitmap())
            }
        }
    }

    private fun createSymbol(locationData: LocationData, imageId: String, mapRefs: MapRefs) {
        mapRefs.symbolManager.create(
                SymbolOptions()
                        .withLatLng(LatLng(locationData.latitude, locationData.longitude))
                        .withIconImage(imageId)
                        .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
        )
    }

    fun zoomToLocation(locationData: LocationData) {
        Timber.d("## Location: zoomToLocation")
        mapRefs?.map?.zoomToLocation(locationData)
    }

    fun getLocationOfMapCenter(): LocationData? =
            mapRefs?.map?.cameraPosition?.target?.let { target ->
                LocationData(
                        latitude = target.latitude,
                        longitude = target.longitude,
                        uncertainty = null
                )
            }
}
