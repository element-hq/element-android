/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThumbnailInfo(
        /**
         * The intended display width of the image in pixels. This may differ from the intrinsic dimensions of the image file.
         */
        @Json(name = "w") val width: Int = 0,

        /**
         * The intended display height of the image in pixels. This may differ from the intrinsic dimensions of the image file.
         */
        @Json(name = "h") val height: Int = 0,

        /**
         * Size of the image in bytes.
         */
        @Json(name = "size") val size: Long = 0,

        /**
         * The mimetype of the image, e.g. "image/jpeg".
         */
        @Json(name = "mimetype") val mimeType: String?
)
