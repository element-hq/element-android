/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.test.fakes.api.FakeSession

internal class FakeSessionManager {

    val instance: SessionManager = mockk()

    init {
        every { instance.getOrCreateSession(any()) } returns fakeSession.instance
    }

    fun assertSessionCreatedWithParams(session: Session, sessionParams: SessionParams) {
        verify { instance.getOrCreateSession(sessionParams) }

        session shouldBeEqualTo fakeSession.instance
    }

    companion object {
        private val fakeSession = FakeSession()
    }
}
