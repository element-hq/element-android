/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.util

import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.internal.util.unescapeHtml

object ContentUtils {
    fun extractUsefulTextFromReply(repliedBody: String): String {
        val lines = repliedBody.lines()
        var wellFormed = repliedBody.startsWith(">")
        var endOfPreviousFound = false
        val usefulLines = ArrayList<String>()
        lines.forEach {
            if (it == "") {
                endOfPreviousFound = true
                return@forEach
            }
            if (!endOfPreviousFound) {
                wellFormed = wellFormed && it.startsWith(">")
            } else {
                usefulLines.add(it)
            }
        }
        return usefulLines.joinToString("\n").takeIf { wellFormed } ?: repliedBody
    }

    fun extractUsefulTextFromHtmlReply(repliedBody: String): String {
        if (repliedBody.startsWith(MX_REPLY_START_TAG)) {
            val closingTagIndex = repliedBody.lastIndexOf(MX_REPLY_END_TAG)
            if (closingTagIndex != -1) {
                return repliedBody.substring(closingTagIndex + MX_REPLY_END_TAG.length).trim()
            }
        }
        return repliedBody
    }

    /**
     * Not every client sends a formatted body in the last edited event since this is not required in the
     * [Matrix specification](https://spec.matrix.org/v1.4/client-server-api/#applying-mnew_content).
     * We must ensure there is one so that it is still considered as a reply when rendering the message.
     */
    fun ensureCorrectFormattedBodyInTextReply(messageTextContent: MessageTextContent, originalFormattedBody: String): MessageTextContent {
        return when {
            messageTextContent.formattedBody != null &&
                    !messageTextContent.formattedBody.contains(MX_REPLY_END_TAG) &&
                    originalFormattedBody.contains(MX_REPLY_END_TAG) -> {
                // take previous formatted body with the new body content
                val newFormattedBody = originalFormattedBody.replaceAfterLast(MX_REPLY_END_TAG, messageTextContent.body)
                messageTextContent.copy(
                        formattedBody = newFormattedBody,
                        format = MessageFormat.FORMAT_MATRIX_HTML,
                )
            }
            else -> messageTextContent
        }
    }

    @Suppress("RegExpRedundantEscape")
    fun formatSpoilerTextFromHtml(formattedBody: String): String {
        // var reason = "",
        // can capture the spoiler reason for better formatting? ex. { reason = it.value;  ">"}
        return formattedBody.replace("(?<=<span data-mx-spoiler)=\\\".+?\\\">".toRegex(), ">")
                .replace("(?<=<span data-mx-spoiler>).+?(?=</span>)".toRegex()) { SPOILER_CHAR.repeat(it.value.length) }
                .unescapeHtml()
    }

    private const val SPOILER_CHAR = "█"
    private const val MX_REPLY_START_TAG = "<mx-reply>"
    private const val MX_REPLY_END_TAG = "</mx-reply>"
}
