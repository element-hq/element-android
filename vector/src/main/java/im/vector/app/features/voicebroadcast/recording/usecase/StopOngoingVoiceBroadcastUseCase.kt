/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
