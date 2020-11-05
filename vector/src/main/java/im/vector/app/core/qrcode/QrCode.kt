/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.core.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

fun String.toBitMatrix(size: Int): BitMatrix {
    return QRCodeWriter().encode(
            this,
            BarcodeFormat.QR_CODE,
            size,
            size
    )
}

fun BitMatrix.toBitmap(@ColorInt backgroundColor: Int = Color.WHITE,
                       @ColorInt foregroundColor: Int = Color.BLACK): Bitmap {
    val colorBuffer = IntArray(width * height)
    var rowOffset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val arrayIndex = x + rowOffset
            colorBuffer[arrayIndex] = if (get(x, y)) foregroundColor else backgroundColor
        }
        rowOffset += width
    }

    return Bitmap.createBitmap(colorBuffer, width, height, Bitmap.Config.ARGB_8888)
}
