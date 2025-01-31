/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.reporting

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ReportContentBody(
        /**
         * Required. The score to rate this content as where -100 is most offensive and 0 is inoffensive.
         */
        @Json(name = "score") val score: Int,

        /**
         * Required. The reason the content is being reported. May be blank.
         */
        @Json(name = "reason") val reason: String
)
