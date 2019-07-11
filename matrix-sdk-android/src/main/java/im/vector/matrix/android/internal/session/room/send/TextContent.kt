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

package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType

/**
 * Contains a text and eventually a formatted text
 */
data class TextContent(
        val text: String,

        val formattedText: String? = null
) {
    fun takeFormatted() = formattedText ?: text
}


fun TextContent.toMessageTextContent(): MessageTextContent {
    return MessageTextContent(
            type = MessageType.MSGTYPE_TEXT,
            format = MessageType.FORMAT_MATRIX_HTML.takeIf { formattedText != null },
            body = text,
            formattedBody = formattedText
    )
}

fun TextContent.removeInReplyFallbacks(): TextContent {
    return copy(
            text = extractUsefulTextFromReply(this.text),
            formattedText = this.formattedText?.let { extractUsefulTextFromHtmlReply(it) }
    )
}

private fun extractUsefulTextFromReply(repliedBody: String): String {
    val lines = repliedBody.lines()
    var wellFormed = repliedBody.startsWith(">")
    var endOfPreviousFound = false
    val usefullines = ArrayList<String>()
    lines.forEach {
        if (it == "") {
            endOfPreviousFound = true
            return@forEach
        }
        if (!endOfPreviousFound) {
            wellFormed = wellFormed && it.startsWith(">")
        } else {
            usefullines.add(it)
        }
    }
    return usefullines.joinToString("\n").takeIf { wellFormed } ?: repliedBody
}

private fun extractUsefulTextFromHtmlReply(repliedBody: String): String {
    if (repliedBody.startsWith("<mx-reply>")) {
        return repliedBody.substring(repliedBody.lastIndexOf("</mx-reply>") + "</mx-reply>".length).trim()
    }
    return repliedBody
}
