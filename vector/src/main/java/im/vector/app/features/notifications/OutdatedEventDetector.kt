/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import im.vector.app.ActiveSessionDataSource
import org.matrix.android.sdk.api.session.getRoom
import javax.inject.Inject

class OutdatedEventDetector @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource
) {

    /**
     * Returns true if the given event is outdated.
     * Used to clean up notifications if a displayed message has been read on an
     * other device.
     */
    fun isMessageOutdated(notifiableEvent: NotifiableEvent): Boolean {
        val session = activeSessionDataSource.currentValue?.orNull() ?: return false

        if (notifiableEvent is NotifiableMessageEvent) {
            val eventID = notifiableEvent.eventId
            val roomID = notifiableEvent.roomId
            val room = session.getRoom(roomID) ?: return false
            return room.readService().isEventRead(eventID)
        }
        return false
    }
}
