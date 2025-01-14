/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.getRoom
import javax.inject.Inject

/**
 * Get the list of live (not ended) voice broadcast events in the given room.
 */
class GetRoomLiveVoiceBroadcastsUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val getVoiceBroadcastStateEventUseCase: GetVoiceBroadcastStateEventUseCase,
) {

    fun execute(roomId: String): List<VoiceBroadcastEvent> {
        val session = activeSessionHolder.getSafeActiveSession() ?: return emptyList()
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        return room.stateService().getStateEvents(
                setOf(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO),
                QueryStringValue.IsNotEmpty
        )
                .mapNotNull { stateEvent -> stateEvent.asVoiceBroadcastEvent()?.voiceBroadcastId }
                .mapNotNull { voiceBroadcastId -> getVoiceBroadcastStateEventUseCase.execute(VoiceBroadcast(voiceBroadcastId, roomId)) }
                .filter { it.isLive }
    }
}
