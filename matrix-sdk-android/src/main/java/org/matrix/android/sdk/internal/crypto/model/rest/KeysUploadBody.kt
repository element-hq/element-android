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
import org.matrix.android.sdk.api.util.JsonDict

/**
 * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-keys-upload
 */
@JsonClass(generateAdapter = true)
internal data class KeysUploadBody(
        /**
         * Identity keys for the device.
         *
         * May be absent if no new identity keys are required.
         */
        @Json(name = "device_keys")
        val deviceKeys: DeviceKeys? = null,

        /**
         * One-time public keys for "pre-key" messages. The names of the properties should be in the
         * format <algorithm>:<key_id>. The format of the key is determined by the key algorithm.
         *
         * May be absent if no new one-time keys are required.
         */
        @Json(name = "one_time_keys")
        val oneTimeKeys: JsonDict? = null,

        /**
         * If the user had previously uploaded a fallback key for a given algorithm, it is replaced.
         * The server will only keep one fallback key per algorithm for each user.
         */
        @Json(name = "org.matrix.msc2732.fallback_keys")
        val fallbackKeys: JsonDict? = null
)
