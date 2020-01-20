/*

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

/**
 * This class represents the response to /keys/query request made by downloadKeysForUsers
 *
 * After uploading cross-signing keys, they will be included under the /keys/query endpoint under the master_keys,
 * self_signing_keys and user_signing_keys properties.
 *
 * The user_signing_keys property will only be included when a user requests their own keys.
 */
@JsonClass(generateAdapter = true)
data class KeysQueryResponse(
        /**
         * The device keys per devices per users.
         * Map from userId to map from deviceId to MXDeviceInfo
         * TODO Use MXUsersDevicesMap?
         */
        @Json(name = "device_keys")
        var deviceKeys: Map<String, Map<String, RestDeviceInfo>>? = null,

        /**
         * The failures sorted by homeservers. TODO Bad comment ?
         * TODO Use MXUsersDevicesMap?
         */
        var failures: Map<String, Map<String, Any>>? = null,

        @Json(name = "master_keys")
        var masterKeys: Map<String, RestKeyInfo?>? = null,

        @Json(name = "self_signing_keys")
        var selfSigningKeys: Map<String, RestKeyInfo?>? = null,

        @Json(name = "user_signing_keys")
        var userSigningKeys: Map<String, RestKeyInfo?>? = null

)
