/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class GetTurnServerTask : Task<Unit, TurnServerResponse>

internal class DefaultGetTurnServerTask @Inject constructor(
        private val voipApi: VoipApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetTurnServerTask() {

    override suspend fun execute(params: Unit): TurnServerResponse {
        return executeRequest(globalErrorReceiver) {
            voipApi.getTurnServer()
        }
    }
}
