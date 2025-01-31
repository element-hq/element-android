/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber

object ImageUtils {

    fun getBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
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

    fun getOrientation(context: Context, uri: Uri): Int {
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
        return orientation
    }
}
