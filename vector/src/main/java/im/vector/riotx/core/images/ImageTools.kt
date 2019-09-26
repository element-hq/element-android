/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.core.images

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageTools @Inject constructor(private val context: Context) {


    /**
     * Gets the [ExifInterface] value for the orientation for this local bitmap Uri.
     *
     * @param uri The URI to find the orientation for.  Must be local.
     * @return    The orientation value, which may be [ExifInterface.ORIENTATION_UNDEFINED].
     */
    fun getOrientationForBitmap(uri: Uri): Int {
        var orientation = ExifInterface.ORIENTATION_UNDEFINED

        if (uri.scheme == "content") {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, proj, null, null, null)
                if (cursor != null && cursor.count > 0) {
                    cursor.moveToFirst()
                    val idxData = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val path = cursor.getString(idxData)
                    if (path.isNullOrBlank()) {
                        Timber.w("Cannot find path in media db for uri $uri")
                        return orientation
                    }
                    val exif = ExifInterface(path)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                }
            } catch (e: Exception) {
                // eg SecurityException from com.google.android.apps.photos.content.GooglePhotosImageProvider URIs
                // eg IOException from trying to parse the returned path as a file when it is an http uri.
                Timber.e(e, "Cannot get orientation for bitmap")
            } finally {
                cursor?.close()
            }
        } else if (uri.scheme == "file") {
            try {
                val exif = ExifInterface(uri.path)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            } catch (e: Exception) {
                Timber.e(e, "Cannot get EXIF for file uri $uri")
            }

        }

        return orientation
    }
}