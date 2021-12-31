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

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// SyncResponse represents the request response for server sync v2.
@JsonClass(generateAdapter = true)
data class SyncResponse(
        /**
         * The user private data.
         */
        @Json(name = "account_data") val accountData: UserAccountDataSync? = null,

        /**
         * The opaque token for the end.
         */
        @Json(name = "next_batch") val nextBatch: String? = null,

        /**
         * The updates to the presence status of other users.
         */
        @Json(name = "presence") val presence: PresenceSyncResponse? = null,

        /*
         * Data directly sent to one of user's devices.
         */
        @Json(name = "to_device") val toDevice: ToDeviceSyncResponse? = null,

        /**
         * List of rooms.
         */
        @Json(name = "rooms") val rooms: RoomsSyncResponse? = null,

        /**
         * Devices list update
         */
        @Json(name = "device_lists") val deviceLists: DeviceListResponse? = null,

        /**
         * One time keys management
         */
        @Json(name = "device_one_time_keys_count")
        val deviceOneTimeKeysCount: DeviceOneTimeKeysCountSyncResponse? = null,

        /**
         * The key algorithms for which the server has an unused fallback key for the device.
         * If the client wants the server to have a fallback key for a given key algorithm,
         * but that algorithm is not listed in device_unused_fallback_key_types, the client will upload a new key.
         */
        @Json(name = "org.matrix.msc2732.device_unused_fallback_key_types")
        val deviceUnusedFallbackKeyTypes: List<String>? = null,

        /**
         * List of groups.
         */
        @Json(name = "groups") val groups: GroupsSyncResponse? = null

)
