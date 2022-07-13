/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
