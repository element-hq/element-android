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

import im.vector.app.features.voicebroadcast.STATE_ROOM_VOICE_BROADCAST_INFO
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room
import timber.log.Timber
import javax.inject.Inject

class StartVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String) {
        val room = session.roomService().getRoom(roomId) ?: return

        Timber.d("## StartVoiceBroadcastUseCase: Start voice broadcast requested")

        val onGoingVoiceBroadcastEvents = room.stateService().getStateEvents(
                setOf(STATE_ROOM_VOICE_BROADCAST_INFO),
                QueryStringValue.IsNotEmpty
        )
                .mapNotNull { it.asVoiceBroadcastEvent() }
                .filter { it.content?.voiceBroadcastState != VoiceBroadcastState.STOPPED }

        if (onGoingVoiceBroadcastEvents.isEmpty()) {
            startVoiceBroadcast(room)
        } else {
            Timber.d("## StartVoiceBroadcastUseCase: Cannot start voice broadcast: currentVoiceBroadcastEvents=$onGoingVoiceBroadcastEvents")
        }
    }

    private suspend fun startVoiceBroadcast(room: Room) {
        Timber.d("## StartVoiceBroadcastUseCase: Send new voice broadcast info state event")
        room.stateService().sendStateEvent(
                eventType = STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        voiceBroadcastStateStr = VoiceBroadcastState.STARTED.value,
                        chunkLength = 5L, // TODO Get length from voice broadcast settings
                ).toContent()
        )

        // TODO start recording audio files
    }
}
