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
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView

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
    val emitterConfig = Emitter(2000).perSecond(100)
    val party = Party(
            emitter = emitterConfig,
            colors = confettiColors.toColorInt(context),
            angle = Angle.Companion.BOTTOM,
            spread = Spread.ROUND,
            shapes = listOf(Shape.Square, Shape.Circle),
            size = listOf(Size(12)),
            speed = 2f,
            maxSpeed = 5f,
            fadeOutEnabled = true,
            timeToLive = 2000L,
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
    )
    reset()
    start(party)
}

@ColorInt
private fun List<Int>.toColorInt(context: Context) = map { ContextCompat.getColor(context, it) }
