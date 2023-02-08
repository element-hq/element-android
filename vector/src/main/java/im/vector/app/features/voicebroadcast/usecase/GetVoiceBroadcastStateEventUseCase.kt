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

import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastStateEventUseCase @Inject constructor(
        private val session: Session,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? {
        val room = session.getRoom(voiceBroadcast.roomId) ?: error("Unknown roomId: ${voiceBroadcast.roomId}")
        return getMostRecentRelatedEvent(room, voiceBroadcast)
                .also { event ->
                    Timber.d(
                            "## VoiceBroadcast | " +
                                    "voiceBroadcastId=${event?.voiceBroadcastId}, " +
                                    "state=${event?.content?.voiceBroadcastState}"
                    )
                }
    }

    /**
     * Get the most recent event related to the given voice broadcast.
     */
    private fun getMostRecentRelatedEvent(room: Room, voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? {
        val startedEvent = room.getTimelineEvent(voiceBroadcast.voiceBroadcastId)?.root
        return if (startedEvent?.isRedacted().orTrue()) {
            null
        } else {
            room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                    .mapNotNull { timelineEvent -> timelineEvent.root.asVoiceBroadcastEvent() }
                    .filterNot { it.root.isRedacted() }
                    .maxByOrNull { it.root.originServerTs ?: 0 }
                    ?: startedEvent?.asVoiceBroadcastEvent()
        }
    }
}
