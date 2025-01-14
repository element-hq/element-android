/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.animations

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import im.vector.lib.ui.styles.R
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
