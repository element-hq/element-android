/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.util.Util
import java.nio.ByteBuffer
import java.security.MessageDigest

private const val ADAPTIVE_TRANSFORMATION_ID = "adaptive-icon-transform"
private val ID_BYTES = ADAPTIVE_TRANSFORMATION_ID.toByteArray()

class AdaptiveIconTransformation(private val adaptiveIconSize: Int, private val adaptiveIconOuterSides: Float) : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
        messageDigest.update(ByteBuffer.allocate(8).putInt(adaptiveIconSize).putFloat(adaptiveIconOuterSides).array())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val insetBmp = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(insetBmp)
        canvas.drawBitmap(toTransform, adaptiveIconOuterSides, adaptiveIconOuterSides, null)
        canvas.setBitmap(null)
        return insetBmp
    }

    override fun equals(other: Any?): Boolean {
        return if (other is AdaptiveIconTransformation) {
            other.adaptiveIconSize == adaptiveIconSize && other.adaptiveIconOuterSides == adaptiveIconOuterSides
        } else {
            false
        }
    }

    override fun hashCode() = Util.hashCode(ADAPTIVE_TRANSFORMATION_ID.hashCode(), Util.hashCode(adaptiveIconSize, Util.hashCode(adaptiveIconOuterSides)))
}
