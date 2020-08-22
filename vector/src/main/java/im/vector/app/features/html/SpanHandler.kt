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

import im.vector.app.core.resources.ColorProvider
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler

class SpanHandler(private val colorProvider: ColorProvider) : TagHandler() {

    override fun supportedTags() = listOf("span")

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val mxSpoiler = tag.attributes()["data-mx-spoiler"]
        if (mxSpoiler != null) {
            SpannableBuilder.setSpans(
                    visitor.builder(),
                    SpoilerSpan(colorProvider),
                    tag.start(),
                    tag.end()
            )
        } else {
            // default thing?
        }
    }
}
