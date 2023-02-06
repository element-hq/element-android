/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.home.room.list.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetRoomLiveVoiceBroadcastsUseCase
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class GetLatestPreviewableEventUseCase @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val getRoomLiveVoiceBroadcastsUseCase: GetRoomLiveVoiceBroadcastsUseCase,
        private val vectorPreferences: VectorPreferences,
) {

    fun execute(roomId: String): TimelineEvent? {
        val room = sessionHolder.getSafeActiveSession()?.getRoom(roomId) ?: return null
        val roomSummary = room.roomSummary() ?: return null
        // FIXME Observing live broadcasts results in many db requests,
        //  to prevent performances issues, we only enable this mechanism if the voice broadcast flag is enabled
        return if (vectorPreferences.isVoiceBroadcastEnabled()) {
            getCallEvent(roomSummary)
                    ?: getLiveVoiceBroadcastEvent(room)
                    ?: getDefaultLatestEvent(room, roomSummary)
        } else {
            roomSummary.latestPreviewableEvent
        }
    }

    private fun getCallEvent(roomSummary: RoomSummary): TimelineEvent? {
        return roomSummary.latestPreviewableEvent
                ?.takeIf { it.root.getClearType() == EventType.CALL_INVITE }
    }

    private fun getLiveVoiceBroadcastEvent(room: Room): TimelineEvent? {
        return getRoomLiveVoiceBroadcastsUseCase.execute(room.roomId)
                .lastOrNull()
                ?.voiceBroadcastId
                ?.let { room.getTimelineEvent(it) }
    }

    private fun getDefaultLatestEvent(room: Room, roomSummary: RoomSummary): TimelineEvent? {
        val latestPreviewableEvent = roomSummary.latestPreviewableEvent

        // If the default latest event is a live voice broadcast (paused or resumed), rely to the started event
        val liveVoiceBroadcastEventId = latestPreviewableEvent?.root?.asVoiceBroadcastEvent()?.takeIf { it.isLive }?.voiceBroadcastId
        if (liveVoiceBroadcastEventId != null) {
            return room.getTimelineEvent(liveVoiceBroadcastEventId)
        }

        return latestPreviewableEvent
                ?.takeUnless { it.root.asMessageAudioEvent()?.isVoiceBroadcast().orFalse() } // Skip voice messages related to voice broadcast
    }
}
