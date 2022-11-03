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
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastEventUseCase @Inject constructor(
        private val session: Session,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? {
        val room = session.getRoom(voiceBroadcast.roomId) ?: error("Unknown roomId: ${voiceBroadcast.roomId}")

        Timber.d("## GetVoiceBroadcastUseCase: get voice broadcast $voiceBroadcast")

        val initialEvent = room.timelineService().getTimelineEvent(voiceBroadcast.voiceBroadcastId)?.root?.asVoiceBroadcastEvent()
        val relatedEvents = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                .sortedBy { it.root.originServerTs }
        return relatedEvents.mapNotNull { it.root.asVoiceBroadcastEvent() }.lastOrNull() ?: initialEvent
    }
}
