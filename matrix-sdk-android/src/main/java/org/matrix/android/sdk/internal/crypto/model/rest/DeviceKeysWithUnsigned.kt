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
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo

@JsonClass(generateAdapter = true)
internal data class DeviceKeysWithUnsigned(
        /**
         * Required. The ID of the user the device belongs to. Must match the user ID used when logging in.
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * Required. The ID of the device these keys belong to. Must match the device ID used when logging in.
         */
        @Json(name = "device_id")
        val deviceId: String,

        /**
         * Required. The encryption algorithms supported by this device.
         */
        @Json(name = "algorithms")
        val algorithms: List<String>?,

        /**
         * Required. Public identity keys. The names of the properties should be in the format <algorithm>:<device_id>.
         * The keys themselves should be encoded as specified by the key algorithm.
         */
        @Json(name = "keys")
        val keys: Map<String, String>?,

        /**
         * Required. Signatures for the device key object. A map from user ID, to a map from <algorithm>:<device_id> to the signature.
         * The signature is calculated using the process described at https://matrix.org/docs/spec/appendices.html#signing-json.
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>?,

        /**
         * Additional data added to the device key information by intermediate servers, and not covered by the signatures.
         */
        @Json(name = "unsigned")
        val unsigned: UnsignedDeviceInfo? = null
)
