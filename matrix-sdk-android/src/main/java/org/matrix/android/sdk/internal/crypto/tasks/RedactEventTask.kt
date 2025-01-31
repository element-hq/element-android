/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface RedactEventTask : Task<RedactEventTask.Params, String> {
    data class Params(
            val txID: String,
            val roomId: String,
            val eventId: String,
            val reason: String?
    )
}

internal class DefaultRedactEventTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : RedactEventTask {

    override suspend fun execute(params: RedactEventTask.Params): String {
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.redactEvent(
                    txId = params.txID,
                    roomId = params.roomId,
                    eventId = params.eventId,
                    reason = if (params.reason == null) emptyMap() else mapOf("reason" to params.reason)
            )
        }
        return response.eventId
    }
}
