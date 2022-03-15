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

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import im.vector.app.R
import timber.log.Timber

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

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.MapTilerMapView,
                0,
                0
        ).run {
            try {
                setLocateButtonVisibility(this)
            } finally {
                recycle()
            }
        }
    }

    private fun setLocateButtonVisibility(typedArray: TypedArray) {
        showLocationButton = typedArray.getBoolean(R.styleable.MapTilerMapView_showLocateButton, false)
    }

    /**
     * For location fragments
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
            mapRefs = MapRefs(
                    map,
                    SymbolManager(this, map, style),
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
                contentDescription = context.getString(R.string.a11y_location_share_locate_button)
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                updateLayoutParams<MarginLayoutParams> {
                    val marginHorizontal = context.resources.getDimensionPixelOffset(R.dimen.location_sharing_locate_button_margin_horizontal)
                    val marginVertical = context.resources.getDimensionPixelOffset(R.dimen.location_sharing_locate_button_margin_vertical)
                    setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
                }
                updateLayoutParams<LayoutParams> {
                    gravity = Gravity.TOP or Gravity.END
                }
            }

    private fun adjustCompassButton(map: MapboxMap) {
        locateButton.post {
            val marginTop = locateButton.height + locateButton.marginTop + locateButton.marginBottom
            val marginRight = context.resources.getDimensionPixelOffset(R.dimen.location_sharing_compass_button_margin_horizontal)
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

        safeMapRefs.map.uiSettings.setLogoMargins(0, 0, 0, state.logoMarginBottom)

        val pinDrawable = state.pinDrawable ?: userLocationDrawable
        pinDrawable?.let { drawable ->
            if (!safeMapRefs.style.isFullyLoaded ||
                    safeMapRefs.style.getImage(state.pinId) == null) {
                safeMapRefs.style.addImage(state.pinId, drawable)
            }
        }

        state.userLocationData?.let { locationData ->
            if (!initZoomDone || !state.zoomOnlyOnce) {
                zoomToLocation(locationData.latitude, locationData.longitude)
                initZoomDone = true
            }

            safeMapRefs.symbolManager.deleteAll()
            if (pinDrawable != null && state.showPin) {
                safeMapRefs.symbolManager.create(
                        SymbolOptions()
                                .withLatLng(LatLng(locationData.latitude, locationData.longitude))
                                .withIconImage(state.pinId)
                                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                )
            }
        }
    }

    fun zoomToLocation(latitude: Double, longitude: Double) {
        Timber.d("## Location: zoomToLocation")
        mapRefs?.map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(INITIAL_MAP_ZOOM_IN_PREVIEW)
                .build()
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
