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
package im.vector.riotx.features.html

import android.content.Context
import im.vector.riotx.R
import im.vector.riotx.features.themes.ThemeUtils
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler

class SpanHandler(context: Context) : TagHandler() {

    override fun supportedTags() = listOf("span")

    private val spoilerBgColorHidden: Int = ThemeUtils.getColor(context, R.attr.vctr_spoiler_background_color)
    private val spoilerBgColorRevealed: Int = ThemeUtils.getColor(context, R.attr.vctr_markdown_block_background_color)

    private val textColor: Int = ThemeUtils.getColor(context, R.attr.riotx_text_primary)

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val mxSpoiler = tag.attributes()["data-mx-spoiler"]
        if (mxSpoiler != null) {
            SpannableBuilder.setSpans(
                    visitor.builder(),
                    SpoilerSpan(spoilerBgColorHidden, spoilerBgColorRevealed, textColor),
                    tag.start(),
                    tag.end()
            )
        } else {
            // default thing?
        }
    }
}
