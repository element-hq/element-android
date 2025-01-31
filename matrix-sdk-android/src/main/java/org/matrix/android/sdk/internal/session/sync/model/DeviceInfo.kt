/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class describes the device information.
 */
@JsonClass(generateAdapter = true)
internal data class DeviceInfo(
        /**
         * The owner user id.
         */
        @Json(name = "user_id")
        val userId: String? = null,

        /**
         * The device id.
         */
        @Json(name = "device_id")
        val deviceId: String? = null,

        /**
         * The device display name.
         */
        @Json(name = "display_name")
        val displayName: String? = null,

        /**
         * The last time this device has been seen.
         */
        @Json(name = "last_seen_ts")
        val lastSeenTs: Long = 0,

        /**
         * The last ip address.
         */
        @Json(name = "last_seen_ip")
        val lastSeenIp: String? = null
)
