/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.presence.service.task

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.presence.PresenceAPI
import org.matrix.android.sdk.internal.session.presence.model.GetPresenceResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class GetPresenceTask : Task<GetPresenceTask.Params, GetPresenceResponse> {
    data class Params(
            val userId: String
    )
}

internal class DefaultGetPresenceTask @Inject constructor(
        private val presenceAPI: PresenceAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetPresenceTask() {
    override suspend fun execute(params: Params): GetPresenceResponse {
        return executeRequest(globalErrorReceiver) {
            presenceAPI.getPresence(params.userId)
        }
    }
}
