/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationInfo(
        /**
         * Required. RFC5870 formatted geo uri 'geo:latitude,longitude;uncertainty' like 'geo:40.05,29.24;30' representing this location.
         */
        @Json(name = "uri") val geoUri: String? = null,

        /**
         * Required. A description of the location e.g. 'Big Ben, London, UK', or some kind
         * of content description for accessibility e.g. 'location attachment'.
         */
        @Json(name = "description") val description: String? = null
)
