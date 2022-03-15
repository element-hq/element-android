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

import im.vector.app.BuildConfig
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.isRTL
import im.vector.app.features.raw.wellknown.getElementWellknown
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class UrlMapProvider @Inject constructor(
        private val localeProvider: LocaleProvider,
        private val session: Session,
        private val rawService: RawService
) {
    private val keyParam = "?key=${BuildConfig.mapTilerKey}"

    private val fallbackMapUrl = buildString {
        append(MAP_BASE_URL)
        append(keyParam)
    }

    suspend fun getMapUrl(): String {
        val upstreamMapUrl = tryOrNull { rawService.getElementWellknown(session.sessionParams) }
                ?.getBestMapTileServerConfig()
                ?.mapStyleUrl
        return upstreamMapUrl ?: fallbackMapUrl
    }

    fun buildStaticMapUrl(locationData: LocationData,
                          zoom: Double,
                          width: Int,
                          height: Int): String {
        return buildString {
            append(STATIC_MAP_BASE_URL)
            append(locationData.longitude)
            append(",")
            append(locationData.latitude)
            append(",")
            append(zoom)
            append("/")
            append(width)
            append("x")
            append(height)
            append(".png")
            append(keyParam)
            if (!localeProvider.isRTL()) {
                // On LTR languages we want the legal mentions to be displayed on the bottom left of the image
                append("&attribution=bottomleft")
            }
        }
    }
}
