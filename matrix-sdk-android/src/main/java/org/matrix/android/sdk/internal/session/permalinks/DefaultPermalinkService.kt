/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import javax.inject.Inject

internal class DefaultPermalinkService @Inject constructor(
        private val permalinkFactory: PermalinkFactory
) : PermalinkService {

    override fun createPermalink(event: Event, forceMatrixTo: Boolean): String? {
        return permalinkFactory.createPermalink(event, forceMatrixTo)
    }

    override fun createPermalink(id: String, forceMatrixTo: Boolean): String? {
        return permalinkFactory.createPermalink(id, forceMatrixTo)
    }

    override fun createRoomPermalink(roomId: String, viaServers: List<String>?, forceMatrixTo: Boolean): String? {
        return permalinkFactory.createRoomPermalink(roomId, viaServers, forceMatrixTo)
    }

    override fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean): String {
        return permalinkFactory.createPermalink(roomId, eventId, forceMatrixTo)
    }

    override fun getLinkedId(url: String): String? {
        return permalinkFactory.getLinkedId(url)
    }

    override fun createMentionSpanTemplate(type: PermalinkService.SpanTemplateType, forceMatrixTo: Boolean): String {
        return permalinkFactory.createMentionSpanTemplate(type, forceMatrixTo)
    }
}
