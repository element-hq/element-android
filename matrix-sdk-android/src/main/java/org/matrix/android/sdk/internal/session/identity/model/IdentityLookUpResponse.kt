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
internal data class IdentityLookUpResponse(
        /**
         * Required. Any applicable mappings of addresses to Matrix User IDs. Addresses which do not have associations will
         * not be included, which can make this property be an empty object.
         */
        @Json(name = "mappings")
        val mappings: Map<String, String>
)
