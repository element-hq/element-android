/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model.rest

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
internal data class KeysQueryResponse(
        /**
         * Information on the queried devices. A map from user ID, to a map from device ID to device information.
         * For each device, the information returned will be the same as uploaded via /keys/upload, with the addition of an unsigned property.
         */
        @Json(name = "device_keys")
        val deviceKeys: Map<String, Map<String, DeviceKeysWithUnsigned>>? = null,

        /**
         * If any remote homeservers could not be reached, they are recorded here. The names of the
         * properties are the names of the unreachable servers.
         *
         * If the homeserver could be reached, but the user or device was unknown, no failure is recorded.
         * Instead, the corresponding user or device is missing from the device_keys result.
         */
        val failures: Map<String, Map<String, Any>>? = null,

        @Json(name = "master_keys")
        val masterKeys: Map<String, RestKeyInfo?>? = null,

        @Json(name = "self_signing_keys")
        val selfSigningKeys: Map<String, RestKeyInfo?>? = null,

        @Json(name = "user_signing_keys")
        val userSigningKeys: Map<String, RestKeyInfo?>? = null
)
