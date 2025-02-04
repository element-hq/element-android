/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class CheckIfCanReplyEventUseCase @Inject constructor() {

    fun execute(event: TimelineEvent, messageContent: MessageContent?, actionPermissions: ActionPermissions): Boolean {
        // Only EventType.MESSAGE, EventType.POLL_START, EventType.POLL_END and EventType.STATE_ROOM_BEACON_INFO event types are supported for the moment
        if (event.root.getClearType() !in
                EventType.STATE_ROOM_BEACON_INFO.values +
                EventType.POLL_START.values +
                EventType.POLL_END.values +
                EventType.MESSAGE
        ) return false

        if (!actionPermissions.canSendMessage) return false
        return when (messageContent?.msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_FILE,
            MessageType.MSGTYPE_POLL_START,
            MessageType.MSGTYPE_POLL_END,
            MessageType.MSGTYPE_BEACON_INFO,
            MessageType.MSGTYPE_LOCATION -> true
            else -> false
        }
    }
}
