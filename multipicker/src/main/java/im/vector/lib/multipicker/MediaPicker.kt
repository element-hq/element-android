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

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import im.vector.lib.multipicker.entity.MultiPickerBaseMediaType
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.ImageUtils

/**
 * Image/Video Picker implementation
 */
class MediaPicker : Picker<MultiPickerBaseMediaType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected image/video files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerBaseMediaType> {
        val mediaList = mutableListOf<MultiPickerBaseMediaType>()

        getSelectedUriList(data).forEach { selectedUri ->
            val projection = arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.MIME_TYPE
            )

            context.contentResolver.query(
                    selectedUri,
                    projection,
                    null,
                    null,
                    null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

                if (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    if (mimeType.isMimeTypeVideo()) {
                        var duration = 0L
                        var width = 0
                        var height = 0
                        var orientation = 0

                        context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { pfd ->
                            val mediaMetadataRetriever = MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                            width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                            height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                            orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
                        }

                        mediaList.add(
                                MultiPickerVideoType(
                                        name,
                                        size,
                                        context.contentResolver.getType(selectedUri),
                                        selectedUri,
                                        width,
                                        height,
                                        orientation,
                                        duration
                                )
                        )
                    } else {
                        // Assume it's an image
                        val bitmap = ImageUtils.getBitmap(context, selectedUri)
                        val orientation = ImageUtils.getOrientation(context, selectedUri)

                        mediaList.add(
                                MultiPickerImageType(
                                        name,
                                        size,
                                        context.contentResolver.getType(selectedUri),
                                        selectedUri,
                                        bitmap?.width ?: 0,
                                        bitmap?.height ?: 0,
                                        orientation
                                )
                        )
                    }
                }
            }
        }
        return mediaList
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "video/*, image/*"
            val mimeTypes = arrayOf("image/*", "video/*")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }

    private fun String?.isMimeTypeVideo() = this?.startsWith("video/") == true
}
