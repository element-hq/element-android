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

package im.vector.lib.multipicker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object ImageUtils {

    suspend fun getBitmap(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Cannot decode Bitmap: %s", uri.toString())
                null
            }
        }
    }

    suspend fun getOrientation(context: Context, uri: Uri): Int {
        return withContext(Dispatchers.IO) {
            var orientation = 0
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                try {
                    ExifInterface(inputStream).let {
                        orientation = it.rotationDegrees
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Cannot read orientation: %s", uri.toString())
                }
            }
            orientation
        }
    }
}
