/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.action

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class CheckIfCanRedactEventUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder
) {

    fun execute(event: TimelineEvent, actionPermissions: ActionPermissions): Boolean {
        // Only some event types are supported for the moment
        val canRedactEventTypes: List<String> = listOf(
                EventType.MESSAGE,
                EventType.STICKER,
                VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
        ) +
                EventType.POLL_START.values +
                EventType.STATE_ROOM_BEACON_INFO.values

        return event.root.getClearType() in canRedactEventTypes &&
                // Message sent by the current user can always be redacted, else check permission for messages sent by other users
                (event.root.senderId == activeSessionHolder.getActiveSession().myUserId || actionPermissions.canRedact)
    }
}
