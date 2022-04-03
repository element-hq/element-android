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

package im.vector.app.core.animations

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import im.vector.app.R
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

fun KonfettiView.play() {
    val confettiColors = listOf(
            R.color.palette_azure,
            R.color.palette_grape,
            R.color.palette_verde,
            R.color.palette_polly,
            R.color.palette_melon,
            R.color.palette_aqua,
            R.color.palette_prune,
            R.color.palette_kiwi
    )
    build()
            .addColors(confettiColors.toColorInt(context))
            .setDirection(0.0, 359.0)
            .setSpeed(2f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(Size(12))
            .setPosition(-50f, width + 50f, -50f, -50f)
            .streamFor(150, 3000L)
}

@ColorInt
private fun List<Int>.toColorInt(context: Context) = map { ContextCompat.getColor(context, it) }
