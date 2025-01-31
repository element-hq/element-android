/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import org.matrix.android.sdk.internal.network.executeRequest as internalExecuteRequest

internal interface RequestExecutor {
    suspend fun <DATA> executeRequest(
            globalErrorReceiver: GlobalErrorReceiver?,
            canRetry: Boolean = false,
            maxDelayBeforeRetry: Long = 32_000L,
            maxRetriesCount: Int = 4,
            requestBlock: suspend () -> DATA
    ): DATA
}

internal object DefaultRequestExecutor : RequestExecutor {
    override suspend fun <DATA> executeRequest(
            globalErrorReceiver: GlobalErrorReceiver?,
            canRetry: Boolean,
            maxDelayBeforeRetry: Long,
            maxRetriesCount: Int,
            requestBlock: suspend () -> DATA
    ): DATA {
        return internalExecuteRequest(globalErrorReceiver, canRetry, maxDelayBeforeRetry, maxRetriesCount, requestBlock)
    }
}
