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
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import timber.log.Timber
import javax.inject.Inject

class StopVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String) {
        val room = session.roomService().getRoom(roomId) ?: return

        Timber.d("## StopVoiceBroadcastUseCase: Stop voice broadcast requested")

        val lastVoiceBroadcastEvent = room.stateService().getStateEvent(STATE_ROOM_VOICE_BROADCAST_INFO, QueryStringValue.Equals(session.myUserId))
        when (val voiceBroadcastState = lastVoiceBroadcastEvent?.content.toModel<MessageVoiceBroadcastInfoContent>()?.voiceBroadcastState) {
            VoiceBroadcastState.STARTED,
            VoiceBroadcastState.PAUSED,
            VoiceBroadcastState.RESUMED -> stopVoiceBroadcast(room, lastVoiceBroadcastEvent)
            else -> Timber.d("## StopVoiceBroadcastUseCase: Cannot stop voice broadcast: currentState=$voiceBroadcastState")
        }
    }

    private suspend fun stopVoiceBroadcast(room: Room, event: Event?) {
        Timber.d("## StopVoiceBroadcastUseCase: Send new voice broadcast info state event")
        val lastVoiceBroadcastContent = event?.content.toModel<MessageVoiceBroadcastInfoContent>()
        val relatesTo = if (lastVoiceBroadcastContent?.voiceBroadcastState == VoiceBroadcastState.STARTED) {
            RelationDefaultContent(RelationType.REFERENCE, event?.eventId)
        } else {
            lastVoiceBroadcastContent?.relatesTo
        }
        room.stateService().sendStateEvent(
                eventType = STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        relatesTo = relatesTo,
                        voiceBroadcastStateStr = VoiceBroadcastState.STOPPED.value,
                ).toContent(),
        )

        // TODO stop recording audio files
    }
}
