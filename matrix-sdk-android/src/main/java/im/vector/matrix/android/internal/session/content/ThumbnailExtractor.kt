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

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import java.io.ByteArrayOutputStream
import java.io.File

internal object ThumbnailExtractor {

    class ThumbnailData(
            val width: Int,
            val height: Int,
            val size: Long,
            val bytes: ByteArray,
            val mimeType: String
    )

    fun extractThumbnail(attachment: ContentAttachmentData): ThumbnailData? {
        val file = File(attachment.path)
        if (!file.exists() || !file.isFile) {
            return null
        }
        return if (attachment.type == ContentAttachmentData.Type.VIDEO) {
            extractVideoThumbnail(attachment)
        } else {
            null
        }
    }

    private fun extractVideoThumbnail(attachment: ContentAttachmentData): ThumbnailData? {
        val thumbnail = ThumbnailUtils.createVideoThumbnail(attachment.path, MediaStore.Video.Thumbnails.MINI_KIND)
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
    }
}
