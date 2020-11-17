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
import android.net.Uri
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import im.vector.lib.multipicker.entity.MultiPickerVideoType
//import im.vector.lib.multipicker.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of recording a video with Camera
 */
class RecordVideoPicker {

    /**
     * Start camera by using a ActivityResultLauncher
     * @return Uri of taken video or null if the operation is cancelled.
     */
    fun startWithExpectingFile(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>): Uri? {
        val videoUri = createVideoUri(context)
        val intent = createIntent().apply {
            putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
        }
        activityResultLauncher.launch(intent)
        return videoUri
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken video or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenVideo(context: Context, videoUri: Uri): MultiPickerVideoType? {
        val projection = arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
        )

        context.contentResolver.query(
                videoUri,
                projection,
                null,
                null,
                null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

            if (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                var duration = 0L
                var width = 0
                var height = 0
                var orientation = 0

                context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(pfd.fileDescriptor)
                    duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                    width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
                    height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
                    orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
                }

                return MultiPickerVideoType(
                        name,
                        size,
                        context.contentResolver.getType(videoUri),
                        videoUri,
                        width,
                        height,
                        orientation,
                        duration
                )
            }
        }
        return null
    }

    private fun createIntent(): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    }

    companion object {
        fun createVideoUri(context: Context): Uri {
            val file = createVideoFile(context)
            val authority = context.packageName + ".multipicker.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        }

        private fun createVideoFile(context: Context): File {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = context.filesDir
            return File.createTempFile(
                    "${timeStamp}_", /* prefix */
                    ".mp4", /* suffix */
                    storageDir /* directory */
            )
        }
    }
}
