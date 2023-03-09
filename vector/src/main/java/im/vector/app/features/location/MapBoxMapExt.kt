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

package im.vector.app.features.location

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap

fun MapboxMap?.zoomToLocation(locationData: LocationData, preserveCurrentZoomLevel: Boolean = false) {
    val zoomLevel = if (preserveCurrentZoomLevel && this?.cameraPosition != null) {
        cameraPosition.zoom
    } else {
        INITIAL_MAP_ZOOM_IN_PREVIEW
    }
    val expectedCameraPosition = CameraPosition.Builder()
            .target(LatLng(locationData.latitude, locationData.longitude))
            .zoom(zoomLevel)
            .build()
    val cameraUpdate = CameraUpdateFactory.newCameraPosition(expectedCameraPosition)
    this?.easeCamera(cameraUpdate)
}

fun MapboxMap?.zoomToBounds(latLngBounds: LatLngBounds) {
    this?.getCameraForLatLngBounds(latLngBounds)?.let { camPosition ->
        // unZoom a little to avoid having pins exactly at the edges of the map
        cameraPosition = CameraPosition.Builder(camPosition)
                .zoom((camPosition.zoom - 1).coerceAtLeast(MapboxConstants.MINIMUM_ZOOM.toDouble()))
                .build()
    }
}
