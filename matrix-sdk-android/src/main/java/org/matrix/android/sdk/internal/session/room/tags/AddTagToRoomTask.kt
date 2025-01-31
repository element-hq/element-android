/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.tags

import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface AddTagToRoomTask : Task<AddTagToRoomTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val tag: String,
            val order: Double?
    )
}

internal class DefaultAddTagToRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddTagToRoomTask {

    override suspend fun execute(params: AddTagToRoomTask.Params) {
        executeRequest(globalErrorReceiver) {
            roomAPI.putTag(
                    userId = userId,
                    roomId = params.roomId,
                    tag = params.tag,
                    body = TagBody(
                            order = params.order
                    )
            )
        }
    }
}
