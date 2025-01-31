/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.RequestExecutor

internal class FakeRequestExecutor : RequestExecutor {

    override suspend fun <DATA> executeRequest(
            globalErrorReceiver: GlobalErrorReceiver?,
            canRetry: Boolean,
            maxDelayBeforeRetry: Long,
            maxRetriesCount: Int,
            requestBlock: suspend () -> DATA
    ): DATA {
        return requestBlock()
    }
}
