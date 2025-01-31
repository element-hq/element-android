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
data class AudioInfo(
        /**
         * The mimetype of the audio e.g. "audio/aac".
         */
        @Json(name = "mimetype") val mimeType: String? = null,

        /**
         * The size of the audio clip in bytes.
         */
        @Json(name = "size") val size: Long? = null,

        /**
         * The duration of the audio in milliseconds.
         */
        @Json(name = "duration") val duration: Int? = null
)
