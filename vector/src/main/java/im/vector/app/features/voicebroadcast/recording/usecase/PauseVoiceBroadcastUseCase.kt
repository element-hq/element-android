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

package im.vector.app.features.voicebroadcast.recording.usecase

import im.vector.app.features.session.coroutineScope
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import javax.inject.Inject

class PauseVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
        private val voiceBroadcastRecorder: VoiceBroadcastRecorder?,
) {

    suspend fun execute(roomId: String): Result<Unit> = runCatching {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## PauseVoiceBroadcastUseCase: Pause voice broadcast requested")

        val lastVoiceBroadcastEvent = room.stateService().getStateEvent(
                VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                QueryStringValue.Equals(session.myUserId)
        )?.asVoiceBroadcastEvent()
        when (val voiceBroadcastState = lastVoiceBroadcastEvent?.content?.voiceBroadcastState) {
            VoiceBroadcastState.STARTED,
            VoiceBroadcastState.RESUMED -> pauseVoiceBroadcast(room, lastVoiceBroadcastEvent.reference)
            else -> Timber.d("## PauseVoiceBroadcastUseCase: Cannot pause voice broadcast: currentState=$voiceBroadcastState")
        }
    }

    private suspend fun pauseVoiceBroadcast(room: Room, reference: RelationDefaultContent?, remainingRetry: Int = 3) {
        Timber.d("## PauseVoiceBroadcastUseCase: Send new voice broadcast info state event")

        try {
            // save the last sequence number and immediately pause the recording
            val lastSequence = voiceBroadcastRecorder?.currentSequence

            room.stateService().sendStateEvent(
                    eventType = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                    stateKey = session.myUserId,
                    body = MessageVoiceBroadcastInfoContent(
                            relatesTo = reference,
                            voiceBroadcastStateStr = VoiceBroadcastState.PAUSED.value,
                            lastChunkSequence = lastSequence,
                    ).toContent(),
            )

            voiceBroadcastRecorder?.pauseRecord()
        } catch (e: Failure) {
            if (remainingRetry > 0) {
                voiceBroadcastRecorder?.pauseOnError()
                // Retry if there is no network issue (sync is running well)
                session.flow().liveSyncState()
                        .filter { it is SyncState.Running }
                        .take(1)
                        .onEach { pauseVoiceBroadcast(room, reference, remainingRetry - 1) }
                        .launchIn(session.coroutineScope)
            }
            throw e
        }
    }
}
