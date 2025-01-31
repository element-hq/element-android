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
internal data class KeysClaimResponse(
        /**
         * The requested keys ordered by device by user.
         * TODO Type does not match spec, should be Map<String, JsonDict>
         */
        @Json(name = "one_time_keys")
        val oneTimeKeys: Map<String, Map<String, Map<String, Map<String, Any>>>>? = null
)
