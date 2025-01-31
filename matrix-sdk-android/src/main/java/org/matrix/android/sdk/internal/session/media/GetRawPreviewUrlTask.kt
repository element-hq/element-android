/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetRawPreviewUrlTask : Task<GetRawPreviewUrlTask.Params, JsonDict> {
    data class Params(
            val url: String,
            val timestamp: Long?
    )
}

internal class DefaultGetRawPreviewUrlTask @Inject constructor(
        private val mediaAPI: MediaAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetRawPreviewUrlTask {

    override suspend fun execute(params: GetRawPreviewUrlTask.Params): JsonDict {
        return executeRequest(globalErrorReceiver) {
            mediaAPI.getPreviewUrlData(params.url, params.timestamp)
        }
    }
}
