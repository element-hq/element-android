/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.di.ActiveSessionHolder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.matrix.android.sdk.api.session.Session

class FakeActiveSessionHolder(
        val fakeSession: FakeSession = FakeSession()
) {
    val instance = mockk<ActiveSessionHolder> {
        every { getActiveSession() } returns fakeSession
        every { getSafeActiveSession() } returns fakeSession
    }

    fun expectSetsActiveSession(session: Session) {
        justRun { instance.setActiveSession(session) }
    }

    fun givenGetSafeActiveSessionReturns(session: Session?) {
        every { instance.getSafeActiveSession() } returns session
    }
}
