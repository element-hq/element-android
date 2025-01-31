/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.html

import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler

class CodeTagHandler : TagHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        SpannableBuilder.setSpans(
                visitor.builder(),
                HtmlCodeSpan(visitor.configuration().theme(), false),
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
class CodePreTagHandler : TagHandler() {
    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val htmlCodeSpan = visitor.builder()
                .getSpans(tag.start(), tag.end())
                .firstOrNull {
                    it.what is HtmlCodeSpan
                }
        if (htmlCodeSpan != null) {
            (htmlCodeSpan.what as HtmlCodeSpan).isBlock = true
        }
    }

    override fun supportedTags(): List<String> {
        return listOf("pre")
    }
}
