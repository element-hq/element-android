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
 * Creates location data from a LocationContent
 * "geo:40.05,29.24;30" -> LocationData(40.05, 29.24, 30)
 * @return location data or null if geo uri is not valid
 */
fun MessageLocationContent.toLocationData(): LocationData? {
    return parseGeo(getBestGeoUri())
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
