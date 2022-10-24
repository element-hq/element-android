/*
 * Copyright 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.VoiceBroadcastEventsGroup
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastListeningItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastListeningItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastRecordingItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastRecordingItem_
import im.vector.app.features.voicebroadcast.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class VoiceBroadcastItemFactory @Inject constructor(
        private val session: Session,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val colorProvider: ColorProvider,
        private val drawableProvider: DrawableProvider,
        private val voiceBroadcastRecorder: VoiceBroadcastRecorder?,
        private val voiceBroadcastPlayer: VoiceBroadcastPlayer,
) {

    fun create(
            params: TimelineItemFactoryParams,
            messageContent: MessageVoiceBroadcastInfoContent,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            attributes: AbsMessageItem.Attributes,
    ): VectorEpoxyModel<out VectorEpoxyHolder>? {
        // Only display item of the initial event with updated data
        if (messageContent.voiceBroadcastState != VoiceBroadcastState.STARTED) return null
        val eventsGroup = params.eventsGroup ?: return null
        val voiceBroadcastEventsGroup = VoiceBroadcastEventsGroup(eventsGroup)
        val mostRecentTimelineEvent = voiceBroadcastEventsGroup.getLastDisplayableEvent()
        val mostRecentEvent = mostRecentTimelineEvent.root.asVoiceBroadcastEvent()
        val mostRecentMessageContent = mostRecentEvent?.content ?: return null
        val isRecording = mostRecentMessageContent.voiceBroadcastState != VoiceBroadcastState.STOPPED && mostRecentEvent.root.stateKey == session.myUserId
        val recorderName = mostRecentTimelineEvent.root.stateKey?.let { session.getUser(it) }?.displayName ?: mostRecentTimelineEvent.root.stateKey
        return if (isRecording) {
            createRecordingItem(
                    params.event.roomId,
                    eventsGroup.groupId,
                    highlight,
                    callback,
                    attributes
            )
        } else {
            createListeningItem(
                    params.event.roomId,
                    eventsGroup.groupId,
                    mostRecentMessageContent.voiceBroadcastState,
                    recorderName,
                    highlight,
                    callback,
                    attributes
            )
        }
    }

    private fun createRecordingItem(
            roomId: String,
            voiceBroadcastId: String,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            attributes: AbsMessageItem.Attributes,
    ): MessageVoiceBroadcastRecordingItem {
        val roomSummary = session.getRoom(roomId)?.roomSummary()
        return MessageVoiceBroadcastRecordingItem_()
                .id("voice_broadcast_$voiceBroadcastId")
                .attributes(attributes)
                .highlighted(highlight)
                .roomItem(roomSummary?.toMatrixItem())
                .colorProvider(colorProvider)
                .drawableProvider(drawableProvider)
                .voiceBroadcastRecorder(voiceBroadcastRecorder)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .callback(callback)
    }

    private fun createListeningItem(
            roomId: String,
            voiceBroadcastId: String,
            voiceBroadcastState: VoiceBroadcastState?,
            broadcasterName: String?,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            attributes: AbsMessageItem.Attributes,
    ): MessageVoiceBroadcastListeningItem {
        val roomSummary = session.getRoom(roomId)?.roomSummary()
        return MessageVoiceBroadcastListeningItem_()
                .id("voice_broadcast_$voiceBroadcastId")
                .attributes(attributes)
                .highlighted(highlight)
                .roomItem(roomSummary?.toMatrixItem())
                .colorProvider(colorProvider)
                .drawableProvider(drawableProvider)
                .voiceBroadcastPlayer(voiceBroadcastPlayer)
                .voiceBroadcastId(voiceBroadcastId)
                .voiceBroadcastState(voiceBroadcastState)
                .broadcasterName(broadcasterName)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .callback(callback)
    }
}
