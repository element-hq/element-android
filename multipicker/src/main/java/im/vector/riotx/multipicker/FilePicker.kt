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

package im.vector.riotx.multipicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import im.vector.riotx.multipicker.entity.MultiPickerFileType

class FilePicker(override val requestCode: Int) : Picker<MultiPickerFileType>(requestCode) {

    override fun startWith(activity: Activity) {
        activity.startActivityForResult(createIntent(), requestCode)
    }

    override fun startWith(fragment: Fragment) {
        fragment.startActivityForResult(createIntent(), requestCode)
    }

    override fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<MultiPickerFileType> {
        if (requestCode != this.requestCode && resultCode != Activity.RESULT_OK) {
            return emptyList()
        }

        val fileList = mutableListOf<MultiPickerFileType>()

        val selectedUriList = mutableListOf<Uri>()
        val dataUri = data?.data
        val clipData = data?.clipData

        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                selectedUriList.add(clipData.getItemAt(i).uri)
            }
        } else if (dataUri != null) {
            selectedUriList.add(dataUri)
        } else {
            data?.extras?.get(Intent.EXTRA_STREAM)?.let {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is List<*> -> selectedUriList.addAll(it as List<Uri>)
                    else     -> selectedUriList.add(it as Uri)
                }
            }
        }

        selectedUriList.forEach { selectedUri ->
            context.contentResolver.query(selectedUri, null, null, null, null)
                    ?.use { cursor ->
                        val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            val name = cursor.getString(nameColumn)
                            val size = cursor.getLong(sizeColumn)

                            fileList.add(
                                    MultiPickerFileType(
                                            name,
                                            size,
                                            context.contentResolver.getType(selectedUri),
                                            selectedUri
                                    )
                            )
                        }
                    }
        }
        return fileList
    }

    private fun createIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "*/*"
        }
    }
}
