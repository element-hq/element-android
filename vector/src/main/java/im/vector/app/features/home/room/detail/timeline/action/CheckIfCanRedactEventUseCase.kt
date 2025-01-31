/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class CheckIfCanRedactEventUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder
) {

    fun execute(event: TimelineEvent, actionPermissions: ActionPermissions): Boolean {
        // Only some event types are supported for the moment
        val canRedactEventTypes = listOf(EventType.MESSAGE, EventType.STICKER) +
                EventType.POLL_START + EventType.STATE_ROOM_BEACON_INFO

        return event.root.getClearType() in canRedactEventTypes &&
                // Message sent by the current user can always be redacted, else check permission for messages sent by other users
                (event.root.senderId == activeSessionHolder.getActiveSession().myUserId || actionPermissions.canRedact)
    }
}
