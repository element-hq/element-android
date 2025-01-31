/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes.internal.auth

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.internal.auth.PendingSessionStore

internal class FakePendingSessionStore {

    val instance: PendingSessionStore = mockk()

    init {
        coJustRun { instance.delete() }
    }

    fun verifyPendingSessionDataCleared() {
        coVerify { instance.delete() }
    }
}
