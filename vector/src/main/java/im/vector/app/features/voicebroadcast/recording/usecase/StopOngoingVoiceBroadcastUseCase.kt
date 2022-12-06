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

package im.vector.app.features.voicebroadcast.recording.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.voicebroadcast.VoiceBroadcastHelper
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetRoomLiveVoiceBroadcastsUseCase
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import timber.log.Timber
import javax.inject.Inject

/**
 * Stop ongoing voice broadcast if any.
 */
class StopOngoingVoiceBroadcastUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val getRoomLiveVoiceBroadcastsUseCase: GetRoomLiveVoiceBroadcastsUseCase,
        private val voiceBroadcastHelper: VoiceBroadcastHelper,
) {

    suspend fun execute() {
        Timber.d("## StopOngoingVoiceBroadcastUseCase: Stop ongoing voice broadcast requested")

        val session = activeSessionHolder.getSafeActiveSession() ?: run {
            Timber.w("## StopOngoingVoiceBroadcastUseCase: no active session")
            return
        }
        // FIXME Iterate only on recent rooms for the moment, improve this
        val recentRooms = session.roomService()
                .getBreadcrumbs(roomSummaryQueryParams {
                    displayName = QueryStringValue.NoCondition
                    memberships = listOf(Membership.JOIN)
                })
                .mapNotNull { session.getRoom(it.roomId) }

        recentRooms
                .forEach { room ->
                    val ongoingVoiceBroadcasts = getRoomLiveVoiceBroadcastsUseCase.execute(room.roomId)
                    val myOngoingVoiceBroadcastId = ongoingVoiceBroadcasts.find { it.root.stateKey == session.myUserId }?.reference?.eventId
                    val initialEvent = myOngoingVoiceBroadcastId?.let { room.timelineService().getTimelineEvent(it)?.root?.asVoiceBroadcastEvent() }
                    if (myOngoingVoiceBroadcastId != null && initialEvent?.content?.deviceId == session.sessionParams.deviceId) {
                        voiceBroadcastHelper.stopVoiceBroadcast(room.roomId)
                        return // No need to iterate more as we should not have more than one recording VB
                    }
                }
    }
}
