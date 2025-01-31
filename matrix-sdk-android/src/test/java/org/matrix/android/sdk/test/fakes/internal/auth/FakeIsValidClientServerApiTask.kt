/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes.internal.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.IsValidClientServerApiTask
import org.matrix.android.sdk.internal.auth.IsValidClientServerApiTask.Params

internal class FakeIsValidClientServerApiTask {

    init {
        coEvery { instance.execute(any()) } returns true
    }

    val instance: IsValidClientServerApiTask = mockk()

    fun givenValidationFails() {
        coEvery { instance.execute(any()) } returns false
    }

    fun verifyExecutionWithConfig(config: HomeServerConnectionConfig) {
        coVerify { instance.execute(Params(config)) }
    }

    fun verifyNoExecution() {
        coVerify(inverse = true) { instance.execute(any()) }
    }
}
