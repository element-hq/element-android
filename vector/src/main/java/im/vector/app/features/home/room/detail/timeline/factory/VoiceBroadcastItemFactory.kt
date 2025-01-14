/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.VoiceBroadcastEventsGroup
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageVoiceBroadcastItem
import im.vector.app.features.home.room.detail.timeline.item.BaseEventItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastListeningItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastListeningItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastRecordingItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastRecordingItem_
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class VoiceBroadcastItemFactory @Inject constructor(
        private val session: Session,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val colorProvider: ColorProvider,
        private val drawableProvider: DrawableProvider,
        private val errorFormatter: ErrorFormatter,
        private val voiceBroadcastRecorder: VoiceBroadcastRecorder?,
        private val voiceBroadcastPlayer: VoiceBroadcastPlayer,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val noticeItemFactory: NoticeItemFactory,
) {

    fun create(
            params: TimelineItemFactoryParams,
            messageContent: MessageVoiceBroadcastInfoContent,
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): BaseEventItem<*>? {
        // Only display item of the initial event with updated data
        if (messageContent.voiceBroadcastState != VoiceBroadcastState.STARTED) {
            return noticeItemFactory.create(params)
        }

        val voiceBroadcastEventsGroup = params.eventsGroup?.let { VoiceBroadcastEventsGroup(it) } ?: return null
        val voiceBroadcastEvent = voiceBroadcastEventsGroup.getLastDisplayableEvent().root.asVoiceBroadcastEvent() ?: return null
        val voiceBroadcastContent = voiceBroadcastEvent.content ?: return null
        val voiceBroadcast = VoiceBroadcast(voiceBroadcastId = voiceBroadcastEventsGroup.voiceBroadcastId, roomId = params.event.roomId)

        val isRecording = voiceBroadcastContent.voiceBroadcastState != VoiceBroadcastState.STOPPED &&
                voiceBroadcastEvent.root.stateKey == session.myUserId &&
                messageContent.deviceId == session.sessionParams.deviceId

        val voiceBroadcastAttributes = AbsMessageVoiceBroadcastItem.Attributes(
                voiceBroadcast = voiceBroadcast,
                voiceBroadcastState = voiceBroadcastContent.voiceBroadcastState,
                duration = voiceBroadcastEventsGroup.getDuration(),
                hasUnableToDecryptEvent = voiceBroadcastEventsGroup.hasUnableToDecryptEvent(),
                recorderName = params.event.senderInfo.disambiguatedDisplayName,
                recorder = voiceBroadcastRecorder,
                player = voiceBroadcastPlayer,
                playbackTracker = playbackTracker,
                roomItem = session.getRoom(params.event.roomId)?.roomSummary()?.toMatrixItem(),
                colorProvider = colorProvider,
                drawableProvider = drawableProvider,
                errorFormatter = errorFormatter,
        )

        return if (isRecording) {
            createRecordingItem(highlight, attributes, voiceBroadcastAttributes)
        } else {
            createListeningItem(highlight, attributes, voiceBroadcastAttributes)
        }
    }

    private fun createRecordingItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
            voiceBroadcastAttributes: AbsMessageVoiceBroadcastItem.Attributes,
    ): MessageVoiceBroadcastRecordingItem {
        return MessageVoiceBroadcastRecordingItem_()
                .id("voice_broadcast_${voiceBroadcastAttributes.voiceBroadcast.voiceBroadcastId}")
                .attributes(attributes)
                .voiceBroadcastAttributes(voiceBroadcastAttributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun createListeningItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
            voiceBroadcastAttributes: AbsMessageVoiceBroadcastItem.Attributes,
    ): MessageVoiceBroadcastListeningItem {
        return MessageVoiceBroadcastListeningItem_()
                .id("voice_broadcast_${voiceBroadcastAttributes.voiceBroadcast.voiceBroadcastId}")
                .attributes(attributes)
                .voiceBroadcastAttributes(voiceBroadcastAttributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
