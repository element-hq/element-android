/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.extensions

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.isVoiceBroadcast
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent

fun TimelineEvent.canReact(): Boolean {
    // Only event of type EventType.MESSAGE, EventType.STICKER and EventType.POLL_START, and started voice broadcast are supported for the moment
    return (root.getClearType() in listOf(EventType.MESSAGE, EventType.STICKER) + EventType.POLL_START.values + EventType.POLL_END.values ||
            root.asVoiceBroadcastEvent()?.content?.voiceBroadcastState == VoiceBroadcastState.STARTED) &&
            root.sendState == SendState.SYNCED &&
            !root.isRedacted()
}

/**
 * Get last MessageContent, after a possible edition.
 * This method iterate on the vector event types and fallback to [getLastMessageContent] from the matrix sdk for the other types.
 */
fun TimelineEvent.getVectorLastMessageContent(): MessageContent? {
    // Iterate on event types which are not part of the matrix sdk, otherwise fallback to the sdk method
    return when (root.getClearType()) {
        VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO -> {
            (annotations?.editSummary?.latestEdit?.getClearContent()?.toModel<MessageVoiceBroadcastInfoContent>()
                    ?: root.getClearContent().toModel<MessageVoiceBroadcastInfoContent>())
        }
        else -> getLastMessageContent()
    }
}

fun TimelineEvent.isVoiceBroadcast(): Boolean {
    return root.isVoiceBroadcast()
}
