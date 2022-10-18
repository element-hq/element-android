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

import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventsGroup
import im.vector.app.features.home.room.detail.timeline.helper.VoiceBroadcastEventsGroup
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceBroadcastItem_
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class VoiceBroadcastItemFactory @Inject constructor(
        private val session: Session,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val audioMessagePlaybackTracker: AudioMessagePlaybackTracker,
) {

    fun create(
            messageContent: MessageVoiceBroadcastInfoContent,
            eventsGroup: TimelineEventsGroup?,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            attributes: AbsMessageItem.Attributes,
    ): MessageVoiceBroadcastItem? {
        // Only display item of the initial event with updated data
        if (messageContent.voiceBroadcastState != VoiceBroadcastState.STARTED) return null
        val voiceBroadcastEventsGroup = eventsGroup?.let { VoiceBroadcastEventsGroup(it) } ?: return null
        val mostRecentTimelineEvent = voiceBroadcastEventsGroup.getLastDisplayableEvent()
        val mostRecentEvent = mostRecentTimelineEvent.root.asVoiceBroadcastEvent()
        val mostRecentMessageContent = mostRecentEvent?.content ?: return null
        val isRecording = mostRecentMessageContent.voiceBroadcastState != VoiceBroadcastState.STOPPED && mostRecentEvent.root.stateKey == session.myUserId
        return MessageVoiceBroadcastItem_()
                .attributes(attributes)
                .highlighted(highlight)
                .voiceBroadcastState(mostRecentMessageContent.voiceBroadcastState)
                .recording(isRecording)
                .audioMessagePlaybackTracker(audioMessagePlaybackTracker)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .callback(callback)
    }
}
