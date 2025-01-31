/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.reporting

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ReportContentTask : Task<ReportContentTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val eventId: String,
            val score: Int,
            val reason: String
    )
}

internal class DefaultReportContentTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : ReportContentTask {

    override suspend fun execute(params: ReportContentTask.Params) {
        return executeRequest(globalErrorReceiver) {
            roomAPI.reportContent(params.roomId, params.eventId, ReportContentBody(params.score, params.reason))
        }
    }
}
