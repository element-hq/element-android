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

package im.vector.app.features.home.room.detail.composer.rainbow

import im.vector.app.core.utils.splitEmoji
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Inspired from React-Sdk
 * Ref: https://github.com/matrix-org/matrix-react-sdk/blob/develop/src/utils/colour.js
 */
class RainbowGenerator @Inject constructor() {

    fun generate(text: String): String {
        val split = text.splitEmoji()
        val frequency = 360f / split.size

        return split
                .mapIndexed { idx, letter ->
                    // Do better than React-Sdk: Avoid adding font color for spaces
                    if (letter == " ") {
                        "$letter"
                    } else {
                        val dashColor = hueToRGB(idx * frequency, 1.0f, 0.5f).toDashColor()
                        "<font color=\"$dashColor\">$letter</font>"
                    }
                }
                .joinToString(separator = "")
    }

    private fun hueToRGB(h: Float, s: Float, l: Float): RgbColor {
        val c = s * (1 - abs(2 * l - 1))
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            h < 60f  -> {
                r = c
                g = x
            }
            h < 120f -> {
                r = x
                g = c
            }
            h < 180f -> {
                g = c
                b = x
            }
            h < 240f -> {
                g = x
                b = c
            }
            h < 300f -> {
                r = x
                b = c
            }
            else     -> {
                r = c
                b = x
            }
        }

        return RgbColor(
                ((r + m) * 255).roundToInt(),
                ((g + m) * 255).roundToInt(),
                ((b + m) * 255).roundToInt()
        )
    }
}
