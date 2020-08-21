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

package im.vector.app.core.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import im.vector.app.core.qrcode.toBitMatrix
import im.vector.app.core.qrcode.toBitmap
import kotlin.random.Random

class QrCodeImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var data: String? = null
    private var animate = false

    init {
        setBackgroundColor(Color.WHITE)
    }

    fun setData(data: String, animate: Boolean) {
        this.data = data
        this.animate = animate

        render()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        render()
    }

    private fun render() {
        data
                ?.takeIf { height > 0 }
                ?.let {
                    if (animate) {
                        // NOT SUPPORTED YET val anim = createAnimation(it)
                        // NOT SUPPORTED YET setImageDrawable(anim)
                        // NOT SUPPORTED YET anim.start()
                        // NOT SUPPORTED YET setImageDrawable(BitmapDrawable(resources, it.toBitMatrix(height).toBitmap()))
                        val bitmap = it.toBitMatrix(height).toBitmap()
                        post { setImageBitmap(bitmap) }
                    } else {
                        val bitmap = it.toBitMatrix(height).toBitmap()
                        post { setImageBitmap(bitmap) }
                    }
                }
    }

    private fun createAnimation(data: String): AnimationDrawable {
        val finalQr = data.toBitMatrix(height)

        val list = mutableListOf(finalQr)

        val random = Random(System.currentTimeMillis())
        val repeatTime = 8
        repeat(repeatTime) { index ->
            val alteredQr = finalQr.clone()
            for (x in 0 until alteredQr.width) {
                for (y in 0 until alteredQr.height) {
                    if (random.nextInt(repeatTime - index) == 0) {
                        // Pb is that it does not toggle a whole black square, but only a pixel
                        alteredQr.unset(x, y)
                    }
                }
            }
            list.add(alteredQr)
        }

        val animDrawable = AnimationDrawable()

        list.asReversed()
                .forEach {
                    animDrawable.addFrame(BitmapDrawable(resources, it.toBitmap()), 150)
                }

        return animDrawable
    }
}
