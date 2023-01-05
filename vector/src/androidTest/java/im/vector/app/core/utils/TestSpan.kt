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

package im.vector.app.core.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spannable
import androidx.core.text.getSpans
import im.vector.app.features.html.HtmlCodeSpan
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import me.gujun.android.span.style.CustomTypefaceSpan

fun Spannable.toTestSpan(): String {
    var output = toString()
    readSpansWithContent().forEach {
        val tags = it.span.readTags()
        val remappedContent = it.span.remapContent(source = this, originalContent = it.content)
        output = output.replace(it.content, "${tags.open}$remappedContent${tags.close}")
    }
    return output
}

private fun Spannable.readSpansWithContent() = getSpans<Any>().map { span ->
    val start = getSpanStart(span)
    val end = getSpanEnd(span)
    SpanWithContent(
            content = substring(start, end),
            span = span
    )
}.reversed()

private fun Any.readTags(): SpanTags {
    return when (this::class) {
        OrderedListItemSpan::class -> SpanTags("[list item]", "[/list item]")
        HtmlCodeSpan::class -> SpanTags("[code]", "[/code]")
        StrongEmphasisSpan::class -> SpanTags("[bold]", "[/bold]")
        EmphasisSpan::class, CustomTypefaceSpan::class -> SpanTags("[italic]", "[/italic]")
        else -> throw IllegalArgumentException("Unknown ${this::class}")
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
