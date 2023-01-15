/*
 * Copyright 2023 New Vector Ltd
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

import im.vector.app.ActiveSessionDataSource
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import im.vector.app.features.voicebroadcast.sequence
import org.matrix.android.sdk.api.session.events.model.isVoiceMessage
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class FilteredEventDetector @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource
) {

    /**
     * Returns true if the given event should be ignored.
     * Used to skip notifications if a non expected message is received.
     */
    fun shouldBeIgnored(notifiableEvent: NotifiableEvent): Boolean {
        val session = activeSessionDataSource.currentValue?.orNull() ?: return false

        if (notifiableEvent is NotifiableMessageEvent) {
            val room = session.getRoom(notifiableEvent.roomId) ?: return false
            val timelineEvent = room.getTimelineEvent(notifiableEvent.eventId) ?: return false
            return timelineEvent.shouldBeIgnored()
        }
        return false
    }

    /**
     * Whether the timeline event should be ignored.
     */
    private fun TimelineEvent.shouldBeIgnored(): Boolean {
        if (root.isVoiceMessage()) {
            val audioEvent = root.asMessageAudioEvent()
            // if the event is a voice message related to a voice broadcast, only show the event on the first chunk.
            return audioEvent.isVoiceBroadcast() && audioEvent?.sequence != 1
        }

        return false
    }
}
