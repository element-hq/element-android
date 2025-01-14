/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
