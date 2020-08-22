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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.MultiPickerFileFactory

/**
 * Video Picker implementation
 */
class VideoPicker(override val requestCode: Int) : Picker<MultiPickerVideoType>(requestCode) {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected video files or empty list if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user did not select any files.
     */
    override fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<MultiPickerVideoType> {
        if (requestCode != this.requestCode && resultCode != Activity.RESULT_OK) {
            return emptyList()
        }

        val videoList = mutableListOf<MultiPickerVideoType>()

        getSelectedUriList(data).forEach { selectedUri ->
            val projection = arrayOf(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE
            )

            context.contentResolver.query(
                    selectedUri,
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

                    videoList.add(
                            MultiPickerFileFactory.getVideoObject(
                                    context,
                                    name,
                                    size,
                                    context.contentResolver.getType(selectedUri),
                                    selectedUri
                            )
                    )
                }
            }
        }
        return videoList
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "video/*"
        }
    }
}
