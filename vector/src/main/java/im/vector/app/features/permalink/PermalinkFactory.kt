/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.permalink

import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Contains synchronous methods to create permalinks from the Session.
 */
class PermalinkFactory @Inject constructor(
        private val session: Session,
) {
    fun createPermalinkOfCurrentUser(): String? {
        return createPermalink(session.myUserId)
    }

    fun createPermalink(id: String): String? {
        return session.permalinkService().createPermalink(id)
    }

    fun createPermalink(roomId: String, eventId: String): String {
        return session.permalinkService().createPermalink(roomId, eventId)
    }
}
