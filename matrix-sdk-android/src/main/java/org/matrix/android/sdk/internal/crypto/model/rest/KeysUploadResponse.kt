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
 * This class represents the response to /keys/upload request made by uploadKeys.
 */
@JsonClass(generateAdapter = true)
internal data class KeysUploadResponse(
        /**
         * Required. For each key algorithm, the number of unclaimed one-time keys
         * of that type currently held on the server for this device.
         */
        @Json(name = "one_time_key_counts")
        val oneTimeKeyCounts: Map<String, Int>? = null
) {
    /**
     * Helper methods to extract information from 'oneTimeKeyCounts'.
     *
     * @param algorithm the expected algorithm
     * @return the time key counts
     */
    fun oneTimeKeyCountsForAlgorithm(algorithm: String): Int {
        return oneTimeKeyCounts?.get(algorithm) ?: 0
    }

    /**
     * Tells if there is a oneTimeKeys for a dedicated algorithm.
     *
     * @param algorithm the algorithm
     * @return true if it is found
     */
    fun hasOneTimeKeyCountsForAlgorithm(algorithm: String): Boolean {
        return oneTimeKeyCounts?.containsKey(algorithm) == true
    }
}
