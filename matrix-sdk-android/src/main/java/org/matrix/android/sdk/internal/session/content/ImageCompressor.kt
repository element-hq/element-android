/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.internal.di.SessionDownloadsDirectory
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

internal class ImageCompressor @Inject constructor(
        @SessionDownloadsDirectory
        private val sessionCacheDirectory: File
) {
    private val cacheFolder = File(sessionCacheDirectory, "MF")

    suspend fun compress(
            context: Context,
            imageUri: Uri,
            desiredWidth: Int,
            desiredHeight: Int,
            desiredQuality: Int = 80): Uri {
        return withContext(Dispatchers.IO) {
            val compressedBitmap = BitmapFactory.Options().run {
                inJustDecodeBounds = true
                decodeBitmap(context, imageUri, this)
                inSampleSize = calculateInSampleSize(outWidth, outHeight, desiredWidth, desiredHeight)
                inJustDecodeBounds = false
                decodeBitmap(context, imageUri, this)?.let {
                    rotateBitmap(context, imageUri, it)
                }
            } ?: return@withContext imageUri

            val destinationUri = createDestinationUri(context)

            runCatching {
                context.contentResolver.openOutputStream(destinationUri).use {
                    compressedBitmap.compress(Bitmap.CompressFormat.JPEG, desiredQuality, it)
                }
            }

            return@withContext destinationUri
        }
    }

    private fun rotateBitmap(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            try {
                ExifInterface(inputStream).let { exifInfo ->
                    val orientation = exifInfo.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> {
                            matrix.preRotate(-90f)
                            matrix.preScale(-1f, 1f)
                        }
                        ExifInterface.ORIENTATION_TRANSVERSE -> {
                            matrix.preRotate(90f)
                            matrix.preScale(-1f, 1f)
                        }
                        else                                      -> return bitmap
                    }
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Cannot read orientation: %s", uri.toString())
            }
        }
        return bitmap
    }

    // https://developer.android.com/topic/performance/graphics/load-bitmap
    private fun calculateInSampleSize(width: Int, height: Int, desiredWidth: Int, desiredHeight: Int): Int {
        var inSampleSize = 1

        if (width > desiredWidth || height > desiredHeight) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= desiredHeight && halfWidth / inSampleSize >= desiredWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun decodeBitmap(context: Context, uri: Uri, options: BitmapFactory.Options = BitmapFactory.Options()): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot decode Bitmap")
            null
        }
    }

    private fun createDestinationUri(context: Context): Uri {
        val file = createTempFile()
        val authority = "${context.packageName}.mx-sdk.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun createTempFile(): File {
        if (!cacheFolder.exists()) cacheFolder.mkdirs()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile(
                "${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                cacheFolder /* directory */
        )
    }
}
