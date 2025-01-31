/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            HTML -> "<a href=\"https://matrix.to/#/%1\$s\">%2\$s</a>"
            MARKDOWN -> "[%2\$s](https://matrix.to/#/%1\$s)"
        }
    }
}
