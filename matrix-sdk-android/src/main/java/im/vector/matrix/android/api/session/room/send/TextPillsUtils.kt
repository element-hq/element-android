/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.api.session.room.send

import android.text.SpannableString

/**
 * Utility class to detect special span in CharSequence and turn them into
 * formatted text to send them as a Matrix messages.
 *
 * For now only support UserMentionSpans (TODO rooms, room aliases, etc...)
 */
object TextPillsUtils {

    private const val MENTION_SPAN_TO_HTML_TEMPLATE = "<a href=\"https://matrix.to/#/%1\$s\">%2\$s</a>"

    private const val MENTION_SPAN_TO_MD_TEMPLATE = "[%2\$s](https://matrix.to/#/%1\$s)"

    /**
     * Detects if transformable spans are present in the text.
     * @return the transformed String or null if no Span found
     */
    fun processSpecialSpansToHtml(text: CharSequence): String? {
        return transformPills(text, MENTION_SPAN_TO_HTML_TEMPLATE)
    }

    /**
     * Detects if transformable spans are present in the text.
     * @return the transformed String or null if no Span found
     */
    fun processSpecialSpansToMarkdown(text: CharSequence): String? {
        return transformPills(text, MENTION_SPAN_TO_MD_TEMPLATE)
    }

    private fun transformPills(text: CharSequence, template: String): String? {
        val spannableString = SpannableString.valueOf(text)
        val pills = spannableString
                ?.getSpans(0, text.length, UserMentionSpan::class.java)
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        return buildString {
            var currIndex = 0
            pills.forEachIndexed { _, urlSpan ->
                val start = spannableString.getSpanStart(urlSpan)
                val end = spannableString.getSpanEnd(urlSpan)
                // We want to replace with the pill with a html link
                append(text, currIndex, start)
                append(String.format(template, urlSpan.userId, urlSpan.displayName))
                currIndex = end
            }
        }
    }
}
