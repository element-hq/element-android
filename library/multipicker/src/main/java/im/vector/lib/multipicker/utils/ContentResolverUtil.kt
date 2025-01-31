/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.entity.MultiPickerVideoType

internal fun Uri.toMultiPickerImageType(context: Context): MultiPickerImageType? {
    val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    )

    return context.contentResolver.query(
            this,
            projection,
            null,
            null,
            null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrNull(MediaStore.Images.Media.DISPLAY_NAME) ?: return@use null
        val sizeColumn = cursor.getColumnIndexOrNull(MediaStore.Images.Media.SIZE) ?: return@use null

        if (cursor.moveToNext()) {
            val name = cursor.getStringOrNull(nameColumn)
            val size = cursor.getLongOrNull(sizeColumn) ?: 0

            val bitmap = ImageUtils.getBitmap(context, this)
            val orientation = ImageUtils.getOrientation(context, this)

            MultiPickerImageType(
                    name,
                    size,
                    context.contentResolver.getType(this),
                    this,
                    bitmap?.width ?: 0,
                    bitmap?.height ?: 0,
                    orientation
            )
        } else {
            null
        }
    }
}

internal fun Uri.toMultiPickerVideoType(context: Context): MultiPickerVideoType? {
    val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE
    )

    return context.contentResolver.query(
            this,
            projection,
            null,
            null,
            null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrNull(MediaStore.Video.Media.DISPLAY_NAME) ?: return@use null
        val sizeColumn = cursor.getColumnIndexOrNull(MediaStore.Video.Media.SIZE) ?: return@use null

        if (cursor.moveToNext()) {
            val name = cursor.getStringOrNull(nameColumn)
            val size = cursor.getLongOrNull(sizeColumn) ?: 0
            var duration = 0L
            var width = 0
            var height = 0
            var orientation = 0

            context.contentResolver.openFileDescriptor(this, "r")?.use { pfd ->
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            }

            MultiPickerVideoType(
                    name,
                    size,
                    context.contentResolver.getType(this),
                    this,
                    width,
                    height,
                    orientation,
                    duration
            )
        } else {
            null
        }
    }
}

fun Uri.toMultiPickerAudioType(context: Context): MultiPickerAudioType? {
    val projection = arrayOf(
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE
    )

    return context.contentResolver.query(
            this,
            projection,
            null,
            null,
            null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrNull(MediaStore.Audio.Media.DISPLAY_NAME) ?: return@use null
        val sizeColumn = cursor.getColumnIndexOrNull(MediaStore.Audio.Media.SIZE) ?: return@use null

        if (cursor.moveToNext()) {
            val name = cursor.getStringOrNull(nameColumn)
            val size = cursor.getLongOrNull(sizeColumn) ?: 0
            var duration = 0L

            context.contentResolver.openFileDescriptor(this, "r")?.use { pfd ->
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            }

            MultiPickerAudioType(
                    name,
                    size,
                    sanitize(context.contentResolver.getType(this)),
                    this,
                    duration
            )
        } else {
            null
        }
    }
}

private fun sanitize(type: String?): String? {
    if (type == "application/ogg") {
        // Not supported on old system
        return "audio/ogg"
    }
    return type
}
