/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://github.com/matrix-org/matrix-doc/blob/hs/hash-identity/proposals/2134-identity-hash-lookup.md
 */
@JsonClass(generateAdapter = true)
internal data class IdentityHashDetailResponse(
        /**
         * Required. The pepper the client MUST use in hashing identifiers, and MUST supply to the /lookup endpoint when performing lookups.
         * Servers SHOULD rotate this string often.
         */
        @Json(name = "lookup_pepper")
        val pepper: String,

        /**
         * Required. The algorithms the server supports. Must contain at least "sha256".
         * "none" can be another possible value.
         */
        @Json(name = "algorithms")
        val algorithms: List<String>
) {
    companion object {
        const val ALGORITHM_SHA256 = "sha256"
        const val ALGORITHM_NONE = "none"
    }
}
