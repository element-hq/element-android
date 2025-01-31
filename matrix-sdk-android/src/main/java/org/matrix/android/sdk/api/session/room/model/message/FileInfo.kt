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
data class FileInfo(
        /**
         * The mimetype of the file e.g. application/msword.
         */
        @Json(name = "mimetype") val mimeType: String?,

        /**
         * The size of the file in bytes.
         */
        @Json(name = "size") val size: Long = 0,

        /**
         * Metadata about the image referred to in thumbnail_url.
         */
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,

        /**
         * The URL to the thumbnail of the file. Only present if the thumbnail is unencrypted.
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
fun FileInfo.getThumbnailUrl(): String? {
    return thumbnailFile?.url ?: thumbnailUrl
}
