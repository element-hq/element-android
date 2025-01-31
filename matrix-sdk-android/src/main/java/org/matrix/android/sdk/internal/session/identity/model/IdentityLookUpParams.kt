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
internal data class IdentityLookUpParams(
        /**
         * Required. The addresses to look up. The format of the entries here depend on the algorithm used.
         * Note that queries which have been incorrectly hashed or formatted will lead to no matches.
         */
        @Json(name = "addresses")
        val hashedAddresses: List<String>,

        /**
         * Required. The algorithm the client is using to encode the addresses. This should be one of the available options from /hash_details.
         */
        @Json(name = "algorithm")
        val algorithm: String,

        /**
         * Required. The pepper from /hash_details. This is required even when the algorithm does not make use of it.
         */
        @Json(name = "pepper")
        val pepper: String
)
