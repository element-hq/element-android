/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
