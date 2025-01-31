/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send

import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.model.relation.ReplyToContent
import org.matrix.android.sdk.api.util.ContentUtils.extractUsefulTextFromHtmlReply
import org.matrix.android.sdk.api.util.ContentUtils.extractUsefulTextFromReply
import org.matrix.android.sdk.api.util.TextContent

internal fun TextContent.toMessageTextContent(msgType: String = MessageType.MSGTYPE_TEXT): MessageTextContent {
    return MessageTextContent(
            msgType = msgType,
            format = MessageFormat.FORMAT_MATRIX_HTML.takeIf { formattedText != null },
            body = text,
            formattedBody = formattedText
    )
}

/**
 * Transform a TextContent to a thread message content. It will also add the inReplyTo
 * latestThreadEventId in order for the clients without threads enabled to render it appropriately
 * If latest event not found, we pass rootThreadEventId
 */
internal fun TextContent.toThreadTextContent(
        rootThreadEventId: String,
        latestThreadEventId: String,
        msgType: String = MessageType.MSGTYPE_TEXT
): MessageTextContent {
    return MessageTextContent(
            msgType = msgType,
            format = MessageFormat.FORMAT_MATRIX_HTML.takeIf { formattedText != null },
            body = text,
            relatesTo = RelationDefaultContent(
                    type = RelationType.THREAD,
                    eventId = rootThreadEventId,
                    isFallingBack = true,
                    inReplyTo = ReplyToContent(
                            eventId = latestThreadEventId
                    )
            ),
            formattedBody = formattedText
    )
}

internal fun TextContent.removeInReplyFallbacks(): TextContent {
    return copy(
            text = extractUsefulTextFromReply(this.text),
            formattedText = this.formattedText?.let { extractUsefulTextFromHtmlReply(it) }
    )
}
