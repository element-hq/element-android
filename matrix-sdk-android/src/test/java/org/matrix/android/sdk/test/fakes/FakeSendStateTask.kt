/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.internal.session.room.state.SendStateTask

internal class FakeSendStateTask : SendStateTask by mockk() {

    fun givenExecuteRetryReturns(eventId: String) {
        coEvery { executeRetry(any(), any()) } returns eventId
    }

    fun givenExecuteRetryThrows(error: Throwable) {
        coEvery { executeRetry(any(), any()) } throws error
    }

    fun verifyExecuteRetry(params: SendStateTask.Params, remainingRetry: Int) {
        coVerify { executeRetry(params, remainingRetry) }
    }
}
