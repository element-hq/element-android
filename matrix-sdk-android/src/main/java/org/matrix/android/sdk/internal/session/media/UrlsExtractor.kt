/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import android.util.Patterns
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.isReply
import org.matrix.android.sdk.api.util.ContentUtils
import javax.inject.Inject

internal class UrlsExtractor @Inject constructor() {
    // Sadly Patterns.WEB_URL_WITH_PROTOCOL is not public so filter the protocol later
    private val urlRegex = Patterns.WEB_URL.toRegex()

    fun extract(event: TimelineEvent): List<String> {
        return event.takeIf { it.root.getClearType() == EventType.MESSAGE }
                ?.getLastMessageContent()
                ?.takeIf {
                    it.msgType == MessageType.MSGTYPE_TEXT ||
                            it.msgType == MessageType.MSGTYPE_NOTICE ||
                            it.msgType == MessageType.MSGTYPE_EMOTE
                }
                ?.let { messageContent ->
                    if (event.isReply()) {
                        // This is a reply, strip the reply fallback
                        ContentUtils.extractUsefulTextFromReply(messageContent.body)
                    } else {
                        messageContent.body
                    }
                }
                ?.let { urlRegex.findAll(it) }
                ?.map { it.value }
                ?.filter { it.startsWith("https://") || it.startsWith("http://") }
                ?.distinct()
                ?.toList()
                .orEmpty()
    }
}
