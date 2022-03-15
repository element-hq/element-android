/*
 * Copyright (c) 2022 New Vector Ltd
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
