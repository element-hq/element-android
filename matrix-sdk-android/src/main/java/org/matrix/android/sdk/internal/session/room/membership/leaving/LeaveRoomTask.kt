/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.membership.leaving

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface LeaveRoomTask : Task<LeaveRoomTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val reason: String?
    )
}

internal class DefaultLeaveRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val stateEventDataSource: StateEventDataSource,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource
) : LeaveRoomTask {

    override suspend fun execute(params: LeaveRoomTask.Params) {
        leaveRoom(params.roomId, params.reason)
    }

    private suspend fun leaveRoom(roomId: String, reason: String?) {
        val roomSummary = roomSummaryDataSource.getRoomSummary(roomId)
        if (roomSummary?.membership?.isActive() == false) {
            Timber.v("Room $roomId is not joined so can't be left")
            return
        }
        roomChangeMembershipStateDataSource.updateState(roomId, ChangeMembershipState.Leaving)
        val roomCreateStateEvent = stateEventDataSource.getStateEvent(
                roomId = roomId,
                eventType = EventType.STATE_ROOM_CREATE,
                stateKey = QueryStringValue.IsEmpty,
        )
        // Server is not cleaning predecessor rooms, so we also try to left them
        val predecessorRoomId = roomCreateStateEvent?.getClearContent()?.toModel<RoomCreateContent>()?.predecessor?.roomId
        if (predecessorRoomId != null) {
            leaveRoom(predecessorRoomId, reason)
        }
        try {
            executeRequest(globalErrorReceiver) {
                roomAPI.leave(roomId, mapOf("reason" to reason))
            }
        } catch (failure: Throwable) {
            roomChangeMembershipStateDataSource.updateState(roomId, ChangeMembershipState.FailedLeaving(failure))
            throw failure
        }
    }
}
