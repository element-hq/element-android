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

import android.content.Context
import androidx.core.content.FileProvider
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.attachments.toContentAttachmentData
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastChunk
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class StartVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
        private val voiceBroadcastRecorder: VoiceBroadcastRecorder?,
        private val context: Context,
        private val buildMeta: BuildMeta,
) {

    suspend fun execute(roomId: String): Result<Unit> = runCatching {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## StartVoiceBroadcastUseCase: Start voice broadcast requested")

        val onGoingVoiceBroadcastEvents = room.stateService().getStateEvents(
                setOf(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO),
                QueryStringValue.IsNotEmpty
        )
                .mapNotNull { it.asVoiceBroadcastEvent() }
                .filter { it.content?.voiceBroadcastState != null && it.content?.voiceBroadcastState != VoiceBroadcastState.STOPPED }

        if (onGoingVoiceBroadcastEvents.isEmpty()) {
            startVoiceBroadcast(room)
        } else {
            Timber.d("## StartVoiceBroadcastUseCase: Cannot start voice broadcast: currentVoiceBroadcastEvents=$onGoingVoiceBroadcastEvents")
        }
    }

    private suspend fun startVoiceBroadcast(room: Room) {
        Timber.d("## StartVoiceBroadcastUseCase: Send new voice broadcast info state event")
        val chunkLength = VoiceBroadcastConstants.DEFAULT_CHUNK_LENGTH_IN_SECONDS // Todo Get the length from the room settings
        val eventId = room.stateService().sendStateEvent(
                eventType = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        deviceId = session.sessionParams.deviceId,
                        voiceBroadcastStateStr = VoiceBroadcastState.STARTED.value,
                        chunkLength = chunkLength,
                ).toContent()
        )

        startRecording(room, eventId, chunkLength)
    }

    private fun startRecording(room: Room, eventId: String, chunkLength: Int) {
        voiceBroadcastRecorder?.addListener(object : VoiceBroadcastRecorder.Listener {
            override fun onVoiceMessageCreated(file: File, sequence: Int) {
                sendVoiceFile(room, file, eventId, sequence)
            }
        })
        voiceBroadcastRecorder?.startRecord(room.roomId, chunkLength)
    }

    private fun sendVoiceFile(room: Room, voiceMessageFile: File, referenceEventId: String, sequence: Int) {
        val outputFileUri = FileProvider.getUriForFile(
                context,
                buildMeta.applicationId + ".fileProvider",
                voiceMessageFile,
                "Voice Broadcast Part ($sequence).${voiceMessageFile.extension}"
        )
        val audioType = outputFileUri.toMultiPickerAudioType(context) ?: return
        room.sendService().sendMedia(
                attachment = audioType.toContentAttachmentData(isVoiceMessage = true),
                compressBeforeSending = false,
                roomIds = emptySet(),
                relatesTo = RelationDefaultContent(RelationType.REFERENCE, referenceEventId),
                additionalContent = mapOf(
                        VoiceBroadcastConstants.VOICE_BROADCAST_CHUNK_KEY to VoiceBroadcastChunk(sequence = sequence).toContent()
                )
        )
    }
}
