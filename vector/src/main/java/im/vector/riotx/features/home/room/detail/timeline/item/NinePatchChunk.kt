/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.home.room.detail.timeline.item

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.NinePatchDrawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NinePatchChunk {
    val padding = Rect()

    private var mDivX: IntArray? = null
    private var mDivY: IntArray? = null
    private var mColor: IntArray? = null

    companion object {

        private fun readIntArray(data: IntArray, buffer: ByteBuffer) {
            var i = 0
            val n = data.size
            while (i < n) {
                data[i] = buffer.int
                ++i
            }
        }

        @Throws(RuntimeException::class)
        private fun checkDivCount(length: Int) {
            if (length == 0 || length and 0x01 != 0) {
                throw RuntimeException("invalid nine-patch: $length")
            }
        }

        @Throws(RuntimeException::class)
        private fun deserialize(data: ByteArray): NinePatchChunk? {
            val byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder())

            if (byteBuffer.get().toInt() == 0) return null // is not serialized

            val chunk = NinePatchChunk()
            chunk.mDivX = IntArray(byteBuffer.get().toInt())
            chunk.mDivY = IntArray(byteBuffer.get().toInt())
            chunk.mColor = IntArray(byteBuffer.get().toInt())

            checkDivCount(chunk.mDivX!!.size)
            checkDivCount(chunk.mDivY!!.size)

            // skip 8 bytes
            byteBuffer.int
            byteBuffer.int

            chunk.padding.left = byteBuffer.int
            chunk.padding.right = byteBuffer.int
            chunk.padding.top = byteBuffer.int
            chunk.padding.bottom = byteBuffer.int

            // skip 4 bytes
            byteBuffer.int

            readIntArray(chunk.mDivX!!, byteBuffer)
            readIntArray(chunk.mDivY!!, byteBuffer)
            readIntArray(chunk.mColor!!, byteBuffer)

            return chunk
        }

        fun getNinePatch(context: Context, @DrawableRes resId: Int, @ColorInt color: Int?): NinePatchDrawable? {
            try {
                val myBitmap = BitmapFactory.decodeResource(context.resources, resId)
                val chunk = myBitmap.ninePatchChunk
                val padding = deserialize(chunk)?.padding ?: return null
                return NinePatchDrawable(context.resources, myBitmap, chunk, padding, null).apply {
                    colorFilter = color?.let { PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) }
                }
            } catch (t: Throwable) {
                Timber.e(t, "Failed to get nine patch ")
                return null
            }
        }
    }
}
