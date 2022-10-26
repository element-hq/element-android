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

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.getRoom
import timber.log.Timber
import javax.inject.Inject

class GetOngoingVoiceBroadcastsUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(roomId: String): List<VoiceBroadcastEvent> {
        val session = activeSessionHolder.getSafeActiveSession() ?: run {
            Timber.d("## GetOngoingVoiceBroadcastsUseCase: no active session")
            return emptyList()
        }
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## GetLastVoiceBroadcastUseCase: get last voice broadcast in $roomId")

        return room.stateService().getStateEvents(
                setOf(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO),
                QueryStringValue.IsNotEmpty
        )
                .mapNotNull { it.asVoiceBroadcastEvent() }
                .filter { it.content?.voiceBroadcastState != null && it.content?.voiceBroadcastState != VoiceBroadcastState.STOPPED }
    }
}
