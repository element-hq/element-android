/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.content

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

internal object ThumbnailExtractor {

    class ThumbnailData(
            val width: Int,
            val height: Int,
            val size: Long,
            val bytes: ByteArray,
            val mimeType: String
    )

    fun extractThumbnail(context: Context, attachment: ContentAttachmentData): ThumbnailData? {
        return if (attachment.type == ContentAttachmentData.Type.VIDEO) {
            extractVideoThumbnail(context, attachment)
        } else {
            null
        }
    }

    private fun extractVideoThumbnail(context: Context, attachment: ContentAttachmentData): ThumbnailData? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            mediaMetadataRetriever.setDataSource(context, attachment.queryUri)
            var thumbnail = mediaMetadataRetriever.frameAtTime
            // Scale down the bitmap if it's too large.
            val width: Int = thumbnail.width
            val height: Int = thumbnail.height
            val max = max(width, height)
            if (max > 512) {
                val scale = 512f / max
                val w = (scale * width).roundToInt()
                val h = (scale * height).roundToInt()
                thumbnail = Bitmap.createScaledBitmap(thumbnail, w, h, true)
            }

            val outputStream = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val thumbnailWidth = thumbnail.width
            val thumbnailHeight = thumbnail.height
            val thumbnailSize = outputStream.size()
            val thumbnailData = ThumbnailData(
                    width = thumbnailWidth,
                    height = thumbnailHeight,
                    size = thumbnailSize.toLong(),
                    bytes = outputStream.toByteArray(),
                    mimeType = "image/jpeg"
            )
            thumbnail.recycle()
            outputStream.reset()
            return thumbnailData
        } catch (e: Exception) {
            Timber.e(e, "Cannot extract video thumbnail")
            return null
        }
    }
}
