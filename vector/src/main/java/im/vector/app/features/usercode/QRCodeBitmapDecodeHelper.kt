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
