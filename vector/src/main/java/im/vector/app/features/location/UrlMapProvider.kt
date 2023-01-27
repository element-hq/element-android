/*
 * Copyright (c) 2022 New Vector Ltd
 * Copyright (c) 2022 BWI GmbH
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

import androidx.annotation.VisibleForTesting
import im.vector.app.features.raw.wellknown.WellknownService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlMapProvider @Inject constructor(
        private val wellknownService: WellknownService,
        private val locationSharingConfig: LocationSharingConfig,
) {
    private val keyParam = "?key=${locationSharingConfig.mapTilerKey}"

    @VisibleForTesting
    val fallbackMapUrl = buildString {
        append(MAP_BASE_URL)
        append(keyParam)
    }

    fun getMapStyleUrl() : String {
        return wellknownService.getMapStyleUrl() ?: if (locationSharingConfig.isMapTilerFallbackEnabled) fallbackMapUrl else ""
    }
}
