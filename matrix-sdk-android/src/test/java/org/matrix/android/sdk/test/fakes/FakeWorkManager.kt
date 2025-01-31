/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeWorkManager {

    val instance = mockk<WorkManager>()

    fun expectEnqueueUniqueWork() {
        every { instance.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
    }

    fun verifyEnqueueUniqueWork(workName: String, policy: ExistingWorkPolicy) {
        verify { instance.enqueueUniqueWork(workName, policy, any<OneTimeWorkRequest>()) }
    }

    fun expectCancelUniqueWork() {
        every { instance.cancelUniqueWork(any()) } returns mockk()
    }

    fun verifyCancelUniqueWork(workName: String) {
        verify { instance.cancelUniqueWork(workName) }
    }
}
