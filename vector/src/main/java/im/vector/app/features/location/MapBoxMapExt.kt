/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
