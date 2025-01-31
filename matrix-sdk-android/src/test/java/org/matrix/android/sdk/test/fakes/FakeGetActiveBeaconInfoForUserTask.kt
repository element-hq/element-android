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
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.room.location.GetActiveBeaconInfoForUserTask

internal class FakeGetActiveBeaconInfoForUserTask : GetActiveBeaconInfoForUserTask by mockk() {

    fun givenExecuteReturns(event: Event?) {
        coEvery { execute(any()) } returns event
    }

    fun verifyExecute(params: GetActiveBeaconInfoForUserTask.Params) {
        coVerify { execute(params) }
    }
}
