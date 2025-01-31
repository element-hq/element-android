/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.directory

import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsResponse
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetPublicRoomTask : Task<GetPublicRoomTask.Params, PublicRoomsResponse> {
    data class Params(
            val server: String?,
            val publicRoomsParams: PublicRoomsParams
    )
}

internal class DefaultGetPublicRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetPublicRoomTask {

    override suspend fun execute(params: GetPublicRoomTask.Params): PublicRoomsResponse {
        return executeRequest(globalErrorReceiver) {
            roomAPI.publicRooms(params.server, params.publicRoomsParams)
        }
    }
}
