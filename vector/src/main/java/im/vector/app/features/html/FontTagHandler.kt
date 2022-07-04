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
package im.vector.app.features.html

import android.graphics.Color
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler

/**
 * custom to matrix for IRC-style font coloring.
 */
class FontTagHandler : SimpleTagHandler() {

    override fun supportedTags() = listOf("font")

    override fun getSpans(configuration: MarkwonConfiguration, renderProps: RenderProps, tag: HtmlTag): Any? {
        val mxColorString = tag.attributes()["data-mx-color"]
        val colorString = tag.attributes()["color"]
        val mxBgColorString = tag.attributes()["data-mx-bg-color"]

        val foregroundColor = mxColorString?.let { parseColor(it, Color.BLACK) } ?: colorString?.let { parseColor(it, Color.BLACK) } ?: Color.BLACK

        if (mxBgColorString != null) {
            val backgroundColor = parseColor(mxBgColorString, Color.TRANSPARENT)
            return arrayOf(ForegroundColorSpan(foregroundColor), BackgroundColorSpan(backgroundColor))
        } else {
            return ForegroundColorSpan(foregroundColor)
        }
    }

    private fun parseColor(colorName: String, failResult: Int): Int {
        try {
            return Color.parseColor(colorName)
        } catch (e: Exception) {
            // try other w3c colors?
            return when (colorName) {
                "white" -> Color.WHITE
                "yellow" -> Color.YELLOW
                "fuchsia" -> Color.parseColor("#FF00FF")
                "red" -> Color.RED
                "silver" -> Color.parseColor("#C0C0C0")
                "gray" -> Color.GRAY
                "olive" -> Color.parseColor("#808000")
                "purple" -> Color.parseColor("#800080")
                "maroon" -> Color.parseColor("#800000")
                "aqua" -> Color.parseColor("#00FFFF")
                "lime" -> Color.parseColor("#00FF00")
                "teal" -> Color.parseColor("#008080")
                "green" -> Color.GREEN
                "blue" -> Color.BLUE
                "orange" -> Color.parseColor("#FFA500")
                "navy" -> Color.parseColor("#000080")
                else -> failResult
            }
        }
    }
}
