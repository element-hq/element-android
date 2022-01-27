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
import android.util.AttributeSet
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import im.vector.app.BuildConfig
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

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

    private var isInitializing = AtomicBoolean(false)
    private var mapRefs: MapRefs? = null
    private var initZoomDone = false

    /**
     * For location fragments
     */
    fun initialize() {
        Timber.d("## Location: initialize $isInitializing")
        if (isInitializing.getAndSet(true)) {
            return
        }

        getMapAsync { map ->
            map.setStyle(styleUrl) { style ->
                mapRefs = MapRefs(
                        map,
                        SymbolManager(this, map, style),
                        style
                )
                pendingState?.let { render(it) }
                pendingState = null
            }
        }
    }

    fun render(state: MapState) {
        val safeMapRefs = mapRefs ?: return Unit.also {
            pendingState = state
        }

        state.pinDrawable?.let { pinDrawable ->
            if (safeMapRefs.style.isFullyLoaded &&
                    safeMapRefs.style.getImage(LocationSharingFragment.USER_PIN_NAME) == null) {
                safeMapRefs.style.addImage(LocationSharingFragment.USER_PIN_NAME, pinDrawable)
            }
        }

        state.pinLocationData?.let { locationData ->
            if (!initZoomDone || !state.zoomOnlyOnce) {
                zoomToLocation(locationData.latitude, locationData.longitude)
                initZoomDone = true
            }

            safeMapRefs.symbolManager.create(
                    SymbolOptions()
                            .withLatLng(LatLng(locationData.latitude, locationData.longitude))
                            .withIconImage(LocationSharingFragment.USER_PIN_NAME)
                            .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
            )
        }
    }

    private fun zoomToLocation(latitude: Double, longitude: Double) {
        Timber.d("## Location: zoomToLocation")
        mapRefs?.map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(INITIAL_MAP_ZOOM)
                .build()
    }

    companion object {
        private const val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.mapTilerKey}"
    }
}
