/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.content

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal class ThumbnailExtractor @Inject constructor(
        private val context: Context
) {

    class ThumbnailData(
            val width: Int,
            val height: Int,
            val size: Long,
            val bytes: ByteArray,
            val mimeType: String
    )

    fun extractThumbnail(attachment: ContentAttachmentData): ThumbnailData? {
        return if (attachment.type == ContentAttachmentData.Type.VIDEO) {
            extractVideoThumbnail(attachment)
        } else {
            null
        }
    }

    private fun extractVideoThumbnail(attachment: ContentAttachmentData): ThumbnailData? {
        var thumbnailData: ThumbnailData? = null
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            mediaMetadataRetriever.setDataSource(context, attachment.queryUri)
            mediaMetadataRetriever.frameAtTime?.let { thumbnail ->
                val outputStream = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val thumbnailWidth = thumbnail.width
                val thumbnailHeight = thumbnail.height
                val thumbnailSize = outputStream.size()
                thumbnailData = ThumbnailData(
                        width = thumbnailWidth,
                        height = thumbnailHeight,
                        size = thumbnailSize.toLong(),
                        bytes = outputStream.toByteArray(),
                        mimeType = MimeTypes.Jpeg
                )
                thumbnail.recycle()
                outputStream.reset()
            } ?: run {
                Timber.e("Cannot extract video thumbnail at ${attachment.queryUri}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot extract video thumbnail")
        } finally {
            mediaMetadataRetriever.release()
        }
        return thumbnailData
    }
}
