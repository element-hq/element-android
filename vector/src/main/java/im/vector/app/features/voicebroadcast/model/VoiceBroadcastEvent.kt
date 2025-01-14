/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
