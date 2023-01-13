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

package im.vector.app.features.html

import im.vector.app.features.settings.VectorPreferences
import io.element.android.wysiwyg.spans.InlineCodeSpan
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler

class CodeTagHandler(
        private val vectorPreferences: VectorPreferences
) : TagHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        SpannableBuilder.setSpans(
                visitor.builder(),
                if (vectorPreferences.isRichTextEditorEnabled()) {
                    InlineCodeSpan() // To be removed if this is a code block
                } else {
                    HtmlCodeSpan(visitor.configuration().theme(), false)
                },
                tag.start(),
                tag.end()
        )
    }

    override fun supportedTags(): List<String> {
        return listOf("code")
    }
}

/**
 * Pre tag are already handled by HtmlPlugin to keep the formatting.
 * We are only using it to check for <pre><code>*</code></pre> tags.
 */
class CodePreTagHandler(
        private val vectorPreferences: VectorPreferences
) : TagHandler() {
    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        if (vectorPreferences.isRichTextEditorEnabled()) {
            val inlineCodeSpan = visitor.builder()
                    .getSpans(tag.start(), tag.end())
                    .firstOrNull {
                        it.what is InlineCodeSpan
                    }
            if (inlineCodeSpan != null) {
                SpannableBuilder.setSpans(
                        visitor.builder(),
                        HtmlCodeSpan(visitor.configuration().theme(), true),
                        tag.start(),
                        tag.end()
                )
            }
        } else {
            val htmlCodeSpan = visitor.builder()
                    .getSpans(tag.start(), tag.end())
                    .firstOrNull {
                        it.what is HtmlCodeSpan
                    }
            if (htmlCodeSpan != null) {
                (htmlCodeSpan.what as HtmlCodeSpan).isBlock = true
            }
        }
    }

    override fun supportedTags(): List<String> {
        return listOf("pre")
    }
}
