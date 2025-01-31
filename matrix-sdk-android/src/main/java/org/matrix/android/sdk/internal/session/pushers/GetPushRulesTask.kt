/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetPushRulesTask : Task<GetPushRulesTask.Params, Unit> {
    data class Params(val scope: String)
}

/**
 * We keep this task, but it should not be used anymore, the push rules comes from the sync response.
 */
internal class DefaultGetPushRulesTask @Inject constructor(
        private val pushRulesApi: PushRulesApi,
        private val savePushRulesTask: SavePushRulesTask,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetPushRulesTask {

    override suspend fun execute(params: GetPushRulesTask.Params) {
        val response = executeRequest(globalErrorReceiver) {
            pushRulesApi.getAllRules()
        }

        savePushRulesTask.execute(SavePushRulesTask.Params(response))
    }
}
