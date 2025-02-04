/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.recording.usecase

import android.content.Context
import androidx.core.content.FileProvider
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.attachments.toContentAttachmentData
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastChunk
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.usecase.GetRoomLiveVoiceBroadcastsUseCase
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class StartVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
        private val voiceBroadcastRecorder: VoiceBroadcastRecorder?,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val context: Context,
        private val buildMeta: BuildMeta,
        private val getRoomLiveVoiceBroadcastsUseCase: GetRoomLiveVoiceBroadcastsUseCase,
        private val stopVoiceBroadcastUseCase: StopVoiceBroadcastUseCase,
        private val pauseVoiceBroadcastUseCase: PauseVoiceBroadcastUseCase,
) {

    suspend fun execute(roomId: String): Result<Unit> = runCatching {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## StartVoiceBroadcastUseCase: Start voice broadcast requested")

        assertCanStartVoiceBroadcast(room)
        startVoiceBroadcast(room)
        return Result.success(Unit)
    }

    private suspend fun startVoiceBroadcast(room: Room) {
        Timber.d("## StartVoiceBroadcastUseCase: Send new voice broadcast info state event")
        val chunkLength = VoiceBroadcastConstants.DEFAULT_CHUNK_LENGTH_IN_SECONDS // Todo Get the chunk length from the room settings
        val maxLength = VoiceBroadcastConstants.MAX_VOICE_BROADCAST_LENGTH_IN_SECONDS // Todo Get the max length from the room settings
        val eventId = room.stateService().sendStateEvent(
                eventType = VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        deviceId = session.sessionParams.deviceId,
                        voiceBroadcastStateStr = VoiceBroadcastState.STARTED.value,
                        chunkLength = chunkLength,
                ).toContent()
        )

        val voiceBroadcast = VoiceBroadcast(roomId = room.roomId, voiceBroadcastId = eventId)

        // TODO Update unit test to cover the following line
        room.flow().liveTimelineEvent(eventId).unwrap().first() // wait for the event come back from the sync

        startRecording(room, voiceBroadcast, chunkLength, maxLength)
    }

    private fun startRecording(room: Room, voiceBroadcast: VoiceBroadcast, chunkLength: Int, maxLength: Int) {
        voiceBroadcastRecorder?.addListener(object : VoiceBroadcastRecorder.Listener {
            override fun onVoiceMessageCreated(file: File, sequence: Int) {
                sendVoiceFile(room, file, voiceBroadcast, sequence)
            }

            override fun onRemainingTimeUpdated(remainingTime: Long?) {
                if (remainingTime != null && remainingTime <= 0) {
                    session.coroutineScope.launch { stopVoiceBroadcastUseCase.execute(room.roomId) }
                }
            }

            override fun onStateUpdated(state: VoiceBroadcastRecorder.State) {
                when (state) {
                    VoiceBroadcastRecorder.State.Recording -> playbackTracker.updateCurrentRecording(AudioMessagePlaybackTracker.RECORDING_ID, emptyList())
                    VoiceBroadcastRecorder.State.Idle -> playbackTracker.stopPlaybackOrRecorder(AudioMessagePlaybackTracker.RECORDING_ID)
                    VoiceBroadcastRecorder.State.Error -> {
                        playbackTracker.stopPlaybackOrRecorder(AudioMessagePlaybackTracker.RECORDING_ID)
                        session.coroutineScope.launch { pauseVoiceBroadcastUseCase.execute(room.roomId) }
                    }
                    else -> Unit
                }
            }
        })
        voiceBroadcastRecorder?.startRecordVoiceBroadcast(voiceBroadcast, chunkLength, maxLength)
    }

    private fun sendVoiceFile(room: Room, voiceMessageFile: File, voiceBroadcast: VoiceBroadcast, sequence: Int) {
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
                relatesTo = RelationDefaultContent(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId),
                additionalContent = mapOf(
                        VoiceBroadcastConstants.VOICE_BROADCAST_CHUNK_KEY to VoiceBroadcastChunk(sequence = sequence).toContent()
                )
        )
    }

    private fun assertCanStartVoiceBroadcast(room: Room) {
        assertHasEnoughPowerLevels(room)
        assertNoOngoingVoiceBroadcast(room)
    }

    @VisibleForTesting
    fun assertHasEnoughPowerLevels(room: Room) {
        val powerLevelsHelper = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?.let { PowerLevelsHelper(it) }

        if (powerLevelsHelper?.isUserAllowedToSend(session.myUserId, true, VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO) != true) {
            Timber.d("## StartVoiceBroadcastUseCase: Cannot start voice broadcast: no permission")
            throw VoiceBroadcastFailure.RecordingError.NoPermission
        }
    }

    @VisibleForTesting
    fun assertNoOngoingVoiceBroadcast(room: Room) {
        when {
            voiceBroadcastRecorder?.recordingState == VoiceBroadcastRecorder.State.Recording ||
                    voiceBroadcastRecorder?.recordingState == VoiceBroadcastRecorder.State.Paused -> {
                Timber.d("## StartVoiceBroadcastUseCase: Cannot start voice broadcast: another voice broadcast")
                throw VoiceBroadcastFailure.RecordingError.UserAlreadyBroadcasting
            }
            getRoomLiveVoiceBroadcastsUseCase.execute(room.roomId).isNotEmpty() -> {
                Timber.d("## StartVoiceBroadcastUseCase: Cannot start voice broadcast: user already broadcasting")
                throw VoiceBroadcastFailure.RecordingError.BlockedBySomeoneElse
            }
        }
    }
}
