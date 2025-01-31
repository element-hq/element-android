/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeleteThreePidBody(
        /**
         * Required. The medium of the third party identifier being removed. One of: ["email", "msisdn"]
         */
        @Json(name = "medium") val medium: String,
        /**
         * Required. The third party address being removed.
         */
        @Json(name = "address") val address: String
)
