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
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val uncertainty: Double?
) : Parcelable {

    companion object {

        /**
         * Creates location data from geo uri
         * @param geoUri geo:latitude,longitude;uncertainty
         * @return location data or null if geo uri is not valid
         */
        fun create(geoUri: String): LocationData? {
            val geoParts = geoUri
                    .split(":")
                    .takeIf { it.firstOrNull() == "geo" }
                    ?.getOrNull(1)
                    ?.split(",")

            val latitude = geoParts?.firstOrNull()
            val geoTailParts = geoParts?.getOrNull(1)?.split(";")
            val longitude = geoTailParts?.firstOrNull()
            val uncertainty = geoTailParts?.getOrNull(1)?.replace("u=", "")

            return if (latitude != null && longitude != null) {
                LocationData(
                        latitude = latitude.toDouble(),
                        longitude = longitude.toDouble(),
                        uncertainty = uncertainty?.toDouble()
                )
            } else null
        }
    }
}
