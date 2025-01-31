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
