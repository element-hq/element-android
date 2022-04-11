/*
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
package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.interfaces.DatedObject

/**
 * This class describes the device information
 */
@JsonClass(generateAdapter = true)
data class DeviceInfo(
        /**
         * The owner user id (not documented and useless but the homeserver sent it. You should not need it)
         */
        @Json(name = "user_id")
        val userId: String? = null,

        /**
         * The device id
         */
        @Json(name = "device_id")
        val deviceId: String? = null,

        /**
         * The device display name
         */
        @Json(name = "display_name")
        val displayName: String? = null,

        /**
         * The last time this device has been seen.
         */
        @Json(name = "last_seen_ts")
        val lastSeenTs: Long? = null,

        /**
         * The last ip address
         */
        @Json(name = "last_seen_ip")
        val lastSeenIp: String? = null
) : DatedObject {

    override val date: Long
        get() = lastSeenTs ?: 0
}
