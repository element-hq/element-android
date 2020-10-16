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

package org.matrix.android.sdk.internal.session.room.send

import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.util.ContentUtils.extractUsefulTextFromHtmlReply
import org.matrix.android.sdk.api.util.ContentUtils.extractUsefulTextFromReply

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
            msgType = msgType,
            format = MessageFormat.FORMAT_MATRIX_HTML.takeIf { formattedText != null },
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
