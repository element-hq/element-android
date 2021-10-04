/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo

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
 * Get the url of the encrypted thumbnail or of the thumbnail
 */
fun VideoInfo.getThumbnailUrl(): String? {
        return thumbnailFile?.url ?: thumbnailUrl
}
