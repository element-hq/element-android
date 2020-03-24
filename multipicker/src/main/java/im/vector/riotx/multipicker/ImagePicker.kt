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
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import im.vector.riotx.multipicker.entity.MultiPickerImageType

class ImagePicker(override val requestCode: Int) : Picker<MultiPickerImageType>(requestCode) {

    override fun startWith(activity: Activity) {
        activity.startActivityForResult(createIntent(), requestCode)
    }

    override fun startWith(fragment: Fragment) {
        fragment.startActivityForResult(createIntent(), requestCode)
    }

    override fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<MultiPickerImageType> {
        if (requestCode != this.requestCode && resultCode != Activity.RESULT_OK) {
            return emptyList()
        }

        val imageList = mutableListOf<MultiPickerImageType>()

        getSelectedUriList(data).forEach { selectedUri ->
            val projection = arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE
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

                if (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)

                    var orientation = 0

                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, selectedUri))
                    } else {
                        context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    }

                    context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                        try {
                            ExifInterface(inputStream).let {
                                orientation = it.rotationDegrees
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    imageList.add(
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
        return imageList
    }

    private fun createIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "image/*"
        }
    }
}
