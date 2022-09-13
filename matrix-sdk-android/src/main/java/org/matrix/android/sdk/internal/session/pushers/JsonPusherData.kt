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
package org.matrix.android.sdk.internal.session.pushers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class JsonPusherData(
        /**
         * Required if kind is http. The URL to use to send notifications to.
         * MUST be an HTTPS URL with a path of /_matrix/push/v1/notify.
         */
        @Json(name = "url")
        val url: String? = null,

        /**
         * The format to send notifications in to Push Gateways if the kind is http.
         * Currently the only format available is 'event_id_only'.
         */
        @Json(name = "format")
        val format: String? = null,

        @Json(name = "brand")
        val brand: String? = null
)
