/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

fun BitMatrix.toBitmap(
        @ColorInt backgroundColor: Int = Color.WHITE,
        @ColorInt foregroundColor: Int = Color.BLACK
): Bitmap {
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
