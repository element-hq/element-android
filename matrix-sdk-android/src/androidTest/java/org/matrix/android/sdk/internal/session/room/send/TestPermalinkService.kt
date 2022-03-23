/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.SpanTemplateType.HTML
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.SpanTemplateType.MARKDOWN

class TestPermalinkService : PermalinkService {
    override fun createPermalink(event: Event, forceMatrixTo: Boolean): String? {
        return null
    }

    override fun createPermalink(id: String, forceMatrixTo: Boolean): String? {
        return ""
    }

    override fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean): String {
        return ""
    }

    override fun createRoomPermalink(roomId: String, viaServers: List<String>?, forceMatrixTo: Boolean): String? {
        return null
    }

    override fun getLinkedId(url: String): String? {
        return null
    }

    override fun createMentionSpanTemplate(type: PermalinkService.SpanTemplateType, forceMatrixTo: Boolean): String {
        return when (type) {
            HTML     -> "<a href=\"https://matrix.to/#/%1\$s\">%2\$s</a>"
            MARKDOWN -> "[%2\$s](https://matrix.to/#/%1\$s)"
        }
    }
}
