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

import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.settings.VectorPreferences
import io.element.android.wysiwyg.view.spans.CodeBlockSpan
import io.element.android.wysiwyg.view.spans.InlineCodeSpan
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler

/**
 * Span to be added to any <code> found during initial pass.
 * The actual code spans can then be added on a second pass using this
 * span as a reference.
 */
internal class IntermediateCodeSpan(
        var isBlock: Boolean
)

internal class CodeTagHandler : TagHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        SpannableBuilder.setSpans(
                visitor.builder(), IntermediateCodeSpan(isBlock = false), tag.start(), tag.end()
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
internal class CodePreTagHandler : TagHandler() {
    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val codeSpan = visitor.builder().getSpans(tag.start(), tag.end()).firstOrNull {
            it.what is IntermediateCodeSpan
        }
        if (codeSpan != null) {
            (codeSpan.what as IntermediateCodeSpan).isBlock = true
        }
    }

    override fun supportedTags(): List<String> {
        return listOf("pre")
    }
}

internal class CodePostProcessorTagHandler(
        private val vectorPreferences: VectorPreferences,
        private val dimensionConverter: DimensionConverter,
) : TagHandler() {

    override fun supportedTags() = listOf(HtmlRootTagPlugin.ROOT_TAG_NAME)

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        if (tag.attributes()[HtmlRootTagPlugin.ROOT_ATTRIBUTE] == null) {
            return
        }

        if (tag.isBlock) {
            visitChildren(visitor, renderer, tag.asBlock)
        }

        // Replace any intermediate code spans with the real formatting spans
        visitor.builder()
                .getSpans(tag.start(), tag.end())
                .filter {
                    it.what is IntermediateCodeSpan
                }.forEach { code ->
                    val intermediateCodeSpan = code.what as IntermediateCodeSpan
                    val theme = visitor.configuration().theme()
                    val span = intermediateCodeSpan.toFinalCodeSpan(theme)

                    SpannableBuilder.setSpans(
                            visitor.builder(), span, code.start, code.end
                    )
                }
    }

    private fun IntermediateCodeSpan.toFinalCodeSpan(
            markwonTheme: MarkwonTheme
    ): Any = if (vectorPreferences.isRichTextEditorEnabled()) {
        toRichTextEditorSpan()
    } else {
        HtmlCodeSpan(markwonTheme, isBlock)
    }

    private fun IntermediateCodeSpan.toRichTextEditorSpan() = if (isBlock) {
        CodeBlockSpan(dimensionConverter.dpToPx(10), dimensionConverter.dpToPx(4))
    } else {
        InlineCodeSpan()
    }
}
