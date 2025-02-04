/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent

@Parcelize
data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val uncertainty: Double?
) : Parcelable

/**
 * Creates location data from a MessageLocationContent.
 * "geo:40.05,29.24;u=30" -> LocationData(40.05, 29.24, 30)
 * @return location data or null if geo uri is not valid
 */
fun MessageLocationContent.toLocationData(): LocationData? {
    return parseGeo(getBestGeoUri())
}

/**
 * Creates location data from a geoUri String.
 * "geo:40.05,29.24;u=30" -> LocationData(40.05, 29.24, 30)
 * @return location data or null if geo uri is null or not valid
 */
fun String?.toLocationData(): LocationData? {
    return this?.let { parseGeo(it) }
}

@VisibleForTesting
fun parseGeo(geo: String): LocationData? {
    val geoParts = geo
            .split(":")
            .takeIf { it.firstOrNull() == "geo" }
            ?.getOrNull(1)
            ?.split(";") ?: return null

    val gpsParts = geoParts.getOrNull(0)?.split(",") ?: return null
    val lat = gpsParts.getOrNull(0)?.toDoubleOrNull() ?: return null
    val lng = gpsParts.getOrNull(1)?.toDoubleOrNull() ?: return null

    val uncertainty = geoParts.getOrNull(1)?.replace("u=", "")?.toDoubleOrNull()

    return LocationData(
            latitude = lat,
            longitude = lng,
            uncertainty = uncertainty
    )
}
