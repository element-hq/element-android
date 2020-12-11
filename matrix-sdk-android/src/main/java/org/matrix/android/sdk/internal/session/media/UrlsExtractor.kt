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

package org.matrix.android.sdk.internal.session.media

import android.util.Patterns
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import javax.inject.Inject

internal class UrlsExtractor @Inject constructor() {
    // Sadly Patterns.WEB_URL_WITH_PROTOCOL is not public so filter the protocol later
    private val urlRegex = Patterns.WEB_URL.toRegex()

    fun extract(event: Event): List<String> {
        return event.takeIf { it.getClearType() == EventType.MESSAGE }
                ?.getClearContent()
                ?.toModel<MessageContent>()
                ?.takeIf { it.msgType == MessageType.MSGTYPE_TEXT || it.msgType == MessageType.MSGTYPE_EMOTE }
                ?.body
                ?.let { urlRegex.findAll(it) }
                ?.map { it.value }
                ?.filter { it.startsWith("https://") || it.startsWith("http://") }
                ?.distinct()
                ?.toList()
                .orEmpty()
    }
}
