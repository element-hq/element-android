/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.login.LoginType
import org.matrix.android.sdk.test.fakes.internal.FakeSessionManager
import org.matrix.android.sdk.test.fakes.internal.auth.FakePendingSessionStore
import org.matrix.android.sdk.test.fakes.internal.auth.FakeSessionParamsCreator
import org.matrix.android.sdk.test.fakes.internal.auth.FakeSessionParamsStore
import org.matrix.android.sdk.test.fixtures.CredentialsFixture.aCredentials
import org.matrix.android.sdk.test.fixtures.SessionParamsFixture.aSessionParams

@ExperimentalCoroutinesApi
class DefaultSessionCreatorTest {

    private val fakeSessionParamsStore = FakeSessionParamsStore()
    private val fakeSessionManager = FakeSessionManager()
    private val fakePendingSessionStore = FakePendingSessionStore()
    private val fakeSessionParamsCreator = FakeSessionParamsCreator()

    private val sessionCreator = DefaultSessionCreator(
            fakeSessionParamsStore.instance,
            fakeSessionManager.instance,
            fakePendingSessionStore.instance,
            fakeSessionParamsCreator.instance,
    )

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @Test
    fun `when createSession, then session created`() = runBlockingTest {
        val output = sessionCreator.createSession(credentials, homeServerConnectionConfig, LoginType.UNKNOWN)

        fakePendingSessionStore.verifyPendingSessionDataCleared()
        fakeSessionParamsCreator.verifyCreatedWithParameters(credentials, homeServerConnectionConfig, LoginType.UNKNOWN)
        fakeSessionParamsStore.verifyParamsSaved(sessionParams)
        fakeSessionManager.assertSessionCreatedWithParams(output, sessionParams)
    }

    companion object {
        private val sessionParams = aSessionParams()
        private val credentials = aCredentials()
        private val homeServerConnectionConfig = HomeServerConnectionConfig.Builder().withHomeServerUri("homeserver").build()
    }
}
