/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

// Some helper code from BinaryEye
object QRCodeBitmapDecodeHelper {

    private val multiFormatReader = MultiFormatReader()
    private val decoderHints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

    fun decodeQRFromBitmap(bitmap: Bitmap): Result? =
            decode(bitmap, false) ?: decode(bitmap, true)

    private fun decode(bitmap: Bitmap, invert: Boolean = false): Result? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        return decode(pixels, bitmap, invert)
    }

    private fun decode(
            pixels: IntArray,
            bitmap: Bitmap,
            invert: Boolean = false
    ): Result? {
        val width = bitmap.width
        val height = bitmap.height
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            bitmap
        }.getPixels(pixels, 0, width, 0, 0, width, height)
        return decodeLuminanceSource(
                RGBLuminanceSource(width, height, pixels),
                invert
        )
    }

    private fun decodeLuminanceSource(
            source: LuminanceSource,
            invert: Boolean
    ): Result? {
        return decodeLuminanceSource(
                if (invert) {
                    source.invert()
                } else {
                    source
                }
        )
    }

    private fun decodeLuminanceSource(source: LuminanceSource): Result? {
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            multiFormatReader.decode(bitmap, decoderHints)
        } catch (e: ReaderException) {
            null
        } finally {
            multiFormatReader.reset()
        }
    }
}
