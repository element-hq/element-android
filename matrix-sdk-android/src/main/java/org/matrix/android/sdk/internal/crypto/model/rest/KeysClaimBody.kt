/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents the response to /keys/claim request made by claimOneTimeKeysForUsersDevices.
 */
@JsonClass(generateAdapter = true)
internal data class KeysClaimBody(
        /**
         * The time (in milliseconds) to wait when downloading keys from remote servers. 10 seconds is the recommended default.
         */
        @Json(name = "timeout")
        val timeout: Int? = null,

        /**
         * Required. The keys to be claimed. A map from user ID, to a map from device ID to algorithm name.
         */
        @Json(name = "one_time_keys")
        val oneTimeKeys: Map<String, Map<String, String>>
)
