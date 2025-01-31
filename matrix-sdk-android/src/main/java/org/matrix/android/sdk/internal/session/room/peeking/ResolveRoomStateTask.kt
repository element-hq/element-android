/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.peeking

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ResolveRoomStateTask : Task<ResolveRoomStateTask.Params, List<Event>> {
    data class Params(
            val roomId: String
    )
}

internal class DefaultResolveRoomStateTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : ResolveRoomStateTask {

    override suspend fun execute(params: ResolveRoomStateTask.Params): List<Event> {
        return executeRequest(globalErrorReceiver) {
            roomAPI.getRoomState(params.roomId)
        }
    }
}
