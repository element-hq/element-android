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

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastStateUseCase @Inject constructor(
        private val session: Session,
) {

    fun execute(roomId: String, eventId: String): VoiceBroadcastState? {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## GetVoiceBroadcastStateUseCase: get voice broadcast state requested for $eventId")

        val initialEvent = room.timelineService().getTimelineEvent(eventId)?.root?.asVoiceBroadcastEvent() // Fallback to initial event
        val relatedEvents = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, eventId).sortedBy { it.root.originServerTs }
        val lastVoiceBroadcastEvent = relatedEvents.mapNotNull { it.root.asVoiceBroadcastEvent() }.lastOrNull() ?: initialEvent
        return lastVoiceBroadcastEvent?.content?.voiceBroadcastState
    }
}
