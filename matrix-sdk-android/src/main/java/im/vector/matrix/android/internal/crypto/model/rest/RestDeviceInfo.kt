/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.util.JsonDict

@JsonClass(generateAdapter = true)
internal data class RestDeviceInfo(
        /**
         * The id of this device.
         */
        @Json(name = "device_id")
        val deviceId: String,

        /**
         * the user id
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * The list of algorithms supported by this device.
         */
        @Json(name = "algorithms")
        val algorithms: List<String>? = null,

        /**
         * A map from "<key type>:<deviceId>" to "<base64-encoded key>".
         */
        @Json(name = "keys")
        val keys: Map<String, String>? = null,

        /**
         * The signature of this MXDeviceInfo.
         * A map from "<userId>" to a map from "<key type>:<deviceId>" to "<signature>"
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>? = null,

        /*
         * Additional data from the home server.
         */
        @Json(name = "unsigned")
        val unsigned: JsonDict? = null
)
