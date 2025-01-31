/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.internal.di.WorkManagerProvider

internal class FakeWorkManagerProvider(
        val fakeWorkManager: FakeWorkManager = FakeWorkManager(),
) {

    val instance = mockk<WorkManagerProvider>().also {
        every { it.workManager } returns fakeWorkManager.instance
    }
}
