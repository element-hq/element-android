/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.KeyChangesResponse
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetKeyChangesTask : Task<GetKeyChangesTask.Params, KeyChangesResponse> {
    data class Params(
            // the start token.
            val from: String,
            // the up-to token.
            val to: String
    )
}

internal class DefaultGetKeyChangesTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetKeyChangesTask {

    override suspend fun execute(params: GetKeyChangesTask.Params): KeyChangesResponse {
        return executeRequest(globalErrorReceiver) {
            cryptoApi.getKeyChanges(params.from, params.to)
        }
    }
}
