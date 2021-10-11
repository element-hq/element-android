/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.notifications

import im.vector.app.features.invite.AutoAcceptInvites
import javax.inject.Inject

class NotifiableEventProcessor @Inject constructor(
        private val outdatedDetector: OutdatedEventDetector,
        private val autoAcceptInvites: AutoAcceptInvites
) {

    fun process(eventList: List<NotifiableEvent>, currentRoomId: String?): Map<String, NotifiableEvent?> {
        return eventList.associateBy { it.eventId }
                .mapValues { (_, value) ->
                    when (value) {
                        is InviteNotifiableEvent  -> if (autoAcceptInvites.hideInvites) null else value
                        is NotifiableMessageEvent -> if (shouldIgnoreMessageEventInRoom(currentRoomId, value.roomId) || outdatedDetector.isMessageOutdated(value)) {
                            null
                        } else value
                        is SimpleNotifiableEvent  -> value
                    }
                }
    }

    private fun shouldIgnoreMessageEventInRoom(currentRoomId: String?, roomId: String?): Boolean {
        return currentRoomId != null && roomId == currentRoomId
    }
}
