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
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo

@JsonClass(generateAdapter = true)
data class VideoInfo(
        /**
         * The mimetype of the video e.g. "video/mp4".
         */
        @Json(name = "mimetype") val mimeType: String?,

        /**
         * The width of the video in pixels.
         */
        @Json(name = "w") val width: Int = 0,

        /**
         * The height of the video in pixels.
         */
        @Json(name = "h") val height: Int = 0,

        /**
         * The size of the video in bytes.
         */
        @Json(name = "size") val size: Long = 0,

        /**
         * The duration of the video in milliseconds.
         */
        @Json(name = "duration") val duration: Int = 0,

        /**
         * Metadata about the image referred to in thumbnail_url.
         */
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,

        /**
         * The URL (typically MXC URI) to an image thumbnail of the video clip. Only present if the thumbnail is unencrypted.
         */
        @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,

        /**
         * Information on the encrypted thumbnail file, as specified in End-to-end encryption. Only present if the thumbnail is encrypted.
         */
        @Json(name = "thumbnail_file") val thumbnailFile: EncryptedFileInfo? = null
)

/**
 * Get the url of the encrypted thumbnail or of the thumbnail.
 */
fun VideoInfo.getThumbnailUrl(): String? {
    return thumbnailFile?.url ?: thumbnailUrl
}
