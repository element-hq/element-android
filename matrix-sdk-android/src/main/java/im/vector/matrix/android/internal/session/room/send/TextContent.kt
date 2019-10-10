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
import im.vector.matrix.android.api.util.ContentUtils.extractUsefulTextFromHtmlReply
import im.vector.matrix.android.api.util.ContentUtils.extractUsefulTextFromReply

/**
 * Contains a text and eventually a formatted text
 */
data class TextContent(
        val text: String,

        val formattedText: String? = null
) {
    fun takeFormatted() = formattedText ?: text
}

fun TextContent.toMessageTextContent(msgType: String = MessageType.MSGTYPE_TEXT): MessageTextContent {
    return MessageTextContent(
            type = msgType,
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
