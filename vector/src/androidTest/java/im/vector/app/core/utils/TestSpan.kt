/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.StrikethroughSpan
import androidx.core.text.getSpans
import im.vector.app.features.html.HtmlCodeSpan
import io.element.android.wysiwyg.view.spans.InlineCodeSpan
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import me.gujun.android.span.style.CustomTypefaceSpan

fun Spanned.toTestSpan(): String {
    var output = toString()
    readSpansWithContent().reversed().forEach {
        val tags = it.span.readTags()
        val remappedContent = it.span.remapContent(source = this, originalContent = it.content)
        output = output.replace(it.content, "${tags.open}$remappedContent${tags.close}")
    }
    return output
}

private fun Spanned.readSpansWithContent() = getSpans<Any>().map { span ->
    val start = getSpanStart(span)
    val end = getSpanEnd(span)
    SpanWithContent(
            content = substring(start, end),
            span = span
    )
}.reversed()

private fun Any.readTags(): SpanTags {
    val tagName = when (this::class) {
        OrderedListItemSpan::class -> "list item"
        HtmlCodeSpan::class ->
            if ((this as HtmlCodeSpan).isBlock) "code block" else "inline code"
        StrongEmphasisSpan::class -> "bold"
        EmphasisSpan::class, CustomTypefaceSpan::class -> "italic"
        InlineCodeSpan::class -> "inline code"
        StrikethroughSpan::class -> "strikethrough"
        LinkSpan::class -> "link"
        else -> if (this::class.qualifiedName!!.startsWith("android.widget")) {
            null
        } else {
            throw IllegalArgumentException("Unknown ${this::class}")
        }
    }

    return if (tagName == null) {
        SpanTags("", "")
    } else {
        SpanTags("[$tagName]", "[/$tagName]")
    }
}

private fun Any.remapContent(source: CharSequence, originalContent: String): String {
    return when (this::class) {
        OrderedListItemSpan::class -> {
            val prefix = (this as OrderedListItemSpan).collectNumber(source)
            "$prefix$originalContent"
        }
        else -> originalContent
    }
}

private fun OrderedListItemSpan.collectNumber(text: CharSequence): String {
    val fakeCanvas = mockk<Canvas>()
    val fakeLayout = mockk<Layout>()
    justRun { fakeCanvas.drawText(any(), any(), any(), any()) }
    val paint = Paint()
    drawLeadingMargin(fakeCanvas, paint, 0, 0, 0, 0, 0, text, 0, text.length - 1, true, fakeLayout)
    val slot = slot<String>()
    verify { fakeCanvas.drawText(capture(slot), any(), any(), any()) }
    return slot.captured
}

private data class SpanTags(
        val open: String,
        val close: String,
)

private data class SpanWithContent(
        val content: String,
        val span: Any
)
