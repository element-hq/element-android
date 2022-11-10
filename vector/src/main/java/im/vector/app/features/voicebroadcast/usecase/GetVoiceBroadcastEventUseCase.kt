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

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastEventUseCase @Inject constructor(
        private val session: Session,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): Flow<Optional<VoiceBroadcastEvent>> {
        val room = session.getRoom(voiceBroadcast.roomId) ?: error("Unknown roomId: ${voiceBroadcast.roomId}")

        Timber.d("## GetVoiceBroadcastUseCase: get voice broadcast $voiceBroadcast")

        val initialEvent = room.timelineService().getTimelineEvent(voiceBroadcast.voiceBroadcastId)?.root?.asVoiceBroadcastEvent()
        val latestEvent = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                .mapNotNull { it.root.asVoiceBroadcastEvent() }
                .maxByOrNull { it.root.originServerTs ?: 0 }
                ?: initialEvent

        return when (latestEvent?.content?.voiceBroadcastState) {
            null, VoiceBroadcastState.STOPPED -> flowOf(latestEvent.toOptional())
            else -> {
                room.flow()
                        .liveStateEvent(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO, QueryStringValue.Equals(latestEvent.root.stateKey.orEmpty()))
                        .onStart { emit(latestEvent.root.toOptional()) }
                        .distinctUntilChanged()
                        .filter { !it.hasValue() || it.getOrNull()?.asVoiceBroadcastEvent()?.voiceBroadcastId == voiceBroadcast.voiceBroadcastId }
                        .mapOptional { it.asVoiceBroadcastEvent() }
            }
        }
    }
}
