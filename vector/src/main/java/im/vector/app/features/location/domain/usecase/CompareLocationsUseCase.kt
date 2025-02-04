/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.domain.usecase

import com.mapbox.mapboxsdk.geometry.LatLng
import im.vector.app.features.location.LocationData
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Threshold in meters to consider 2 locations as equal.
 */
private const val SAME_LOCATION_THRESHOLD_IN_METERS = 5

/**
 * Use case to check if 2 locations can be considered as equal.
 */
class CompareLocationsUseCase @Inject constructor(
        private val session: Session
) {

    /**
     * Compare the 2 given locations.
     * @return true when they are really close and could be considered as the same location, false otherwise
     */
    suspend fun execute(location1: LocationData, location2: LocationData): Boolean =
            withContext(session.coroutineDispatchers.io) {
                val loc1 = LatLng(location1.latitude, location1.longitude)
                val loc2 = LatLng(location2.latitude, location2.longitude)
                val distance = loc1.distanceTo(loc2)
                distance <= SAME_LOCATION_THRESHOLD_IN_METERS
            }
}
