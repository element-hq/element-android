/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UploadSignaturesTask : Task<UploadSignaturesTask.Params, Unit> {
    data class Params(
            val signatures: Map<String, Map<String, Any>>
    )
}

internal class DefaultUploadSignaturesTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UploadSignaturesTask {

    override suspend fun execute(params: UploadSignaturesTask.Params) {
        val response = executeRequest(
                globalErrorReceiver,
                canRetry = true,
                maxRetriesCount = 10
        ) {
            cryptoApi.uploadSignatures(params.signatures)
        }
        if (response.failures?.isNotEmpty() == true) {
            throw Throwable(response.failures.toString())
        }
    }
}
