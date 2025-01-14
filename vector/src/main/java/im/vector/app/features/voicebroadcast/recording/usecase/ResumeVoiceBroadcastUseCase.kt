/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.recording.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import timber.log.Timber
import javax.inject.Inject

class ResumeVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String): Result<Unit> = runCatching {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## ResumeVoiceBroadcastUseCase: Resume voice broadcast requested")

        val lastVoiceBroadcastEvent = room.stateService().getStateEvent(
                VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                QueryStringValue.Equals(session.myUserId)
        )?.asVoiceBroadcastEvent()
        when (val voiceBroadcastState = lastVoiceBroadcastEvent?.content?.voiceBroadcastState) {
            VoiceBroadcastState.PAUSED -> resumeVoiceBroadcast(room, lastVoiceBroadcastEvent.reference)
            else -> Timber.d("## ResumeVoiceBroadcastUseCase: Cannot resume voice broadcast: currentState=$voiceBroadcastState")
        }
    }

    /**
     * Resume a paused voice broadcast in the given room.
     *
     * @param room the room related to the voice broadcast
     * @param reference reference on the initial voice broadcast state event (ie. state=STARTED)
     */
    private suspend fun resumeVoiceBroadcast(room: Room, reference: RelationDefaultContent?) {
        Timber.d("## ResumeVoiceBroadcastUseCase: Send new voice broadcast info state event")
        room.stateService().sendStateEvent(
                eventType = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        relatesTo = reference,
                        voiceBroadcastStateStr = VoiceBroadcastState.RESUMED.value,
                ).toContent(),
        )
    }
}
