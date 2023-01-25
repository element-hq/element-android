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

package im.vector.app.features.voicebroadcast.model

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

/**
 * [Event] wrapper for [VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO] event type.
 * Provides additional fields and functions related to voice broadcast.
 */
@JvmInline
value class VoiceBroadcastEvent(val root: Event) {

    /**
     * Reference on the initial voice broadcast state event (ie. with [MessageVoiceBroadcastInfoContent.voiceBroadcastState]=[VoiceBroadcastState.STARTED]).
     */
    val reference: RelationDefaultContent?
        get() {
            val voiceBroadcastInfoContent = root.content.toModel<MessageVoiceBroadcastInfoContent>()
            return if (voiceBroadcastInfoContent?.voiceBroadcastState == VoiceBroadcastState.STARTED) {
                RelationDefaultContent(RelationType.REFERENCE, root.eventId)
            } else {
                voiceBroadcastInfoContent?.relatesTo
            }
        }

    /**
     * The mapped [MessageVoiceBroadcastInfoContent] model of the event content.
     */
    val content: MessageVoiceBroadcastInfoContent?
        get() = root.content.toModel()
}

fun Event.isVoiceBroadcast() = type == VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO

/**
 * Map a [VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO] state event to a [VoiceBroadcastEvent].
 */
fun Event.asVoiceBroadcastEvent() = if (isVoiceBroadcast()) VoiceBroadcastEvent(this) else null
