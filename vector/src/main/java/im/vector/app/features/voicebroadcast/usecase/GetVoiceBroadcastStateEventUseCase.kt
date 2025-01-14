/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastStateEventUseCase @Inject constructor(
        private val session: Session,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? {
        val room = session.getRoom(voiceBroadcast.roomId) ?: error("Unknown roomId: ${voiceBroadcast.roomId}")
        return getMostRecentRelatedEvent(room, voiceBroadcast)
                .also { event ->
                    Timber.d(
                            "## VoiceBroadcast | " +
                                    "voiceBroadcastId=${event?.voiceBroadcastId}, " +
                                    "state=${event?.content?.voiceBroadcastState}"
                    )
                }
    }

    /**
     * Get the most recent event related to the given voice broadcast.
     */
    private fun getMostRecentRelatedEvent(room: Room, voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? {
        val startedEvent = room.getTimelineEvent(voiceBroadcast.voiceBroadcastId)?.root
        return if (startedEvent?.isRedacted().orTrue()) {
            null
        } else {
            room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                    .mapNotNull { timelineEvent -> timelineEvent.root.asVoiceBroadcastEvent() }
                    .filterNot { it.root.isRedacted() }
                    .maxByOrNull { it.root.originServerTs ?: 0 }
                    ?: startedEvent?.asVoiceBroadcastEvent()
        }
    }
}
