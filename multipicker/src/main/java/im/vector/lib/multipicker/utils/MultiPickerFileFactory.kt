/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.multipicker.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import im.vector.riotx.multipicker.entity.MultiPickerAudioType
import im.vector.riotx.multipicker.entity.MultiPickerBaseType
import im.vector.riotx.multipicker.entity.MultiPickerFileType
import im.vector.riotx.multipicker.entity.MultiPickerImageType
import im.vector.riotx.multipicker.entity.MultiPickerVideoType

object MultiPickerFileFactory {

    fun getImageObject(context: Context, displayName: String?, size: Long, mimeType : String?, contentUri: Uri) : MultiPickerImageType {
        val bitmap = ImageUtils.getBitmap(context, contentUri)
        val orientation = ImageUtils.getOrientation(context, contentUri)

        return MultiPickerImageType(
                displayName,
                size,
                mimeType,
                contentUri,
                bitmap?.width ?: 0,
                bitmap?.height ?: 0,
                orientation
        )
    }

    fun getAudioObject(context: Context, displayName: String?, size: Long, mimeType : String?, contentUri: Uri) : MultiPickerAudioType {
        var duration = 0L

        context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        }

        return MultiPickerAudioType(
                displayName,
                size,
                mimeType,
                contentUri,
                duration
        )
    }

    fun getVideoObject(context: Context, displayName: String?, size: Long, mimeType : String?, contentUri: Uri) : MultiPickerVideoType {
        var duration = 0L
        var width = 0
        var height = 0
        var orientation = 0

        context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
        }

        return MultiPickerVideoType(
                displayName,
                size,
                mimeType,
                contentUri,
                width,
                height,
                orientation,
                duration
        )
    }

    fun getFileObject(context: Context, displayName: String?, size: Long, contentUri: Uri) : MultiPickerBaseType {
        var mimeType = context.contentResolver.getType(contentUri)

        return when {
            mimeType?.startsWith("image/") == true -> getImageObject(context, displayName, size, mimeType, contentUri)
            mimeType?.startsWith("video/") == true -> getVideoObject(context, displayName, size, mimeType, contentUri)
            mimeType?.startsWith("audio/") == true -> getAudioObject(context, displayName, size, mimeType, contentUri)
            else -> {
                MultiPickerFileType(
                        displayName,
                        size,
                        mimeType,
                        contentUri
                )
            }
        }
    }
}
