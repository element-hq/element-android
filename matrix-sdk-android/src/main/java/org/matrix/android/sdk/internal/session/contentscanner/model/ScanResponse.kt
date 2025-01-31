/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Example:
 * <pre>
 * {
 *      "clean": true,
 *      "info": "File clean at 6/7/2018, 6:02:40 PM"
 *  }
 * </pre>
 * .
 */
@JsonClass(generateAdapter = true)
internal data class ScanResponse(
        @Json(name = "clean") val clean: Boolean,
        /** Human-readable information about the result. */
        @Json(name = "info") val info: String?
)
