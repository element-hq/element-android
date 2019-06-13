/*
 * Copyright 2016 OpenMarket Ltd
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
 * This class represents the response to /keys/query request made by claimOneTimeKeysForUsersDevices.
 */
@JsonClass(generateAdapter = true)
data class KeysClaimBody(

        @Json(name = "timeout")
        var timeout: Int? = null,

        /**
         * Required. The keys to be claimed. A map from user ID, to a map from device ID to algorithm name.
         */
        @Json(name = "one_time_keys")
        var oneTimeKeys: Map<String, Map<String, String>>
)

