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
import android.graphics.drawable.Drawable
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

class MapTilerMapView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : MapView(context, attrs, defStyleAttr), VectorMapView {

    private var map: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var style: Style? = null

    override fun initialize(onMapReady: () -> Unit) {
        getMapAsync { map ->
            map.setStyle(styleUrl) { style ->
                this.symbolManager = SymbolManager(this, map, style)
                this.map = map
                this.style = style
                onMapReady()
            }
        }
    }

    override fun addPinToMap(pinId: String, image: Drawable) {
        style?.addImage(pinId, image)
    }

    override fun updatePinLocation(pinId: String, latitude: Double, longitude: Double) {
        symbolManager?.create(
                SymbolOptions()
                        .withLatLng(LatLng(latitude, longitude))
                        .withIconImage(pinId)
                        .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
        )
    }

    override fun deleteAllPins() {
        symbolManager?.deleteAll()
    }

    override fun zoomToLocation(latitude: Double, longitude: Double, zoom: Double) {
        map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(zoom)
                .build()
    }

    override fun getCurrentZoom(): Double? {
        return map?.cameraPosition?.zoom
    }

    override fun onClick(callback: () -> Unit) {
        map?.addOnMapClickListener {
            callback()
            true
        }
    }

    companion object {
        private const val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.mapTilerKey}"
    }
}
