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
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Inspired from React-Sdk
 * Ref: https://github.com/matrix-org/matrix-react-sdk/blob/develop/src/utils/colour.js
 */
class RainbowGenerator @Inject constructor() {

    fun generate(text: String): String {
        val split = text.splitEmoji()
        val frequency = 2 * Math.PI / split.size

        return split
                .mapIndexed { idx, letter ->
                    // Do better than React-Sdk: Avoid adding font color for spaces
                    if (letter == " ") {
                        "$letter"
                    } else {
                        val (a, b) = generateAB(idx * frequency, 1f)
                        val dashColor = labToRGB(75, a, b).toDashColor()
                        "<font color=\"$dashColor\">$letter</font>"
                    }
                }
                .joinToString(separator = "")
    }

    private fun generateAB(hue: Double, chroma: Float): Pair<Double, Double> {
        val a = chroma * 127 * cos(hue)
        val b = chroma * 127 * sin(hue)

        return Pair(a, b)
    }

    private fun labToRGB(l: Int, a: Double, b: Double): RgbColor {
        // Convert CIELAB to CIEXYZ (D65)
        var y = (l + 16) / 116.0
        val x = adjustXYZ(y + a / 500) * 0.9505
        val z = adjustXYZ(y - b / 200) * 1.0890

        y = adjustXYZ(y)

        // Linear transformation from CIEXYZ to RGB
        val red = 3.24096994 * x - 1.53738318 * y - 0.49861076 * z
        val green = -0.96924364 * x + 1.8759675 * y + 0.04155506 * z
        val blue = 0.05563008 * x - 0.20397696 * y + 1.05697151 * z

        return RgbColor(adjustRGB(red), adjustRGB(green), adjustRGB(blue))
    }

    private fun adjustXYZ(value: Double): Double {
        if (value > 0.2069) {
            return value.pow(3)
        }
        return 0.1284 * value - 0.01771
    }

    private fun gammaCorrection(value: Double): Double {
        // Non-linear transformation to sRGB
        if (value <= 0.0031308) {
            return 12.92 * value
        }
        return 1.055 * value.pow(1 / 2.4) - 0.055
    }

    private fun adjustRGB(value: Double): Int {
        return (gammaCorrection(value)
                .coerceIn(0.0, 1.0) * 255)
                .roundToInt()
    }
}
