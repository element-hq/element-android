/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.session.room.model.RoomStrippedState
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetRoomSummaryTask : Task<GetRoomSummaryTask.Params, RoomStrippedState> {
    data class Params(
            val roomId: String,
            val viaServers: List<String>?
    )
}

internal class DefaultGetRoomSummaryTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetRoomSummaryTask {

    override suspend fun execute(params: GetRoomSummaryTask.Params): RoomStrippedState {
        return executeRequest(globalErrorReceiver) {
            roomAPI.getRoomSummary(params.roomId, params.viaServers)
        }
    }
}
