/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.accountdata

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateRoomAccountDataTask : Task<UpdateRoomAccountDataTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val type: String,
            val content: JsonDict
    )
}

internal class DefaultUpdateRoomAccountDataTask @Inject constructor(
        private val roomApi: RoomAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateRoomAccountDataTask {

    override suspend fun execute(params: UpdateRoomAccountDataTask.Params) {
        return executeRequest(globalErrorReceiver) {
            roomApi.setRoomAccountData(userId, params.roomId, params.type, params.content)
        }
    }
}
