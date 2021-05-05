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
import im.vector.lib.multipicker.entity.MultiPickerBaseMediaType
import im.vector.lib.multipicker.utils.isMimeTypeVideo
import im.vector.lib.multipicker.utils.toMultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType

/**
 * Image/Video Picker implementation
 */
class MediaPicker : Picker<MultiPickerBaseMediaType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected image/video files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerBaseMediaType> {
        return getSelectedUriList(data).mapNotNull { selectedUri ->
            val mimeType = context.contentResolver.getType(selectedUri)

            if (mimeType.isMimeTypeVideo()) {
                selectedUri.toMultiPickerVideoType(context)
            } else {
                // Assume it's an image
                selectedUri.toMultiPickerImageType(context)
            }
        }
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
}
