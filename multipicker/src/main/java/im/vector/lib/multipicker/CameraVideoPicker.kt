/*
 * Copyright (c) 2021 New Vector Ltd
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
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of taking a video with Camera
 */
class CameraVideoPicker {

    /**
     * Start camera by using a ActivityResultLauncher
     * @return Uri of taken photo or null if the operation is cancelled.
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
     * @return Taken photo or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenVideo(context: Context, videoUri: Uri): MultiPickerVideoType? {
        return videoUri.toMultiPickerVideoType(context)
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
