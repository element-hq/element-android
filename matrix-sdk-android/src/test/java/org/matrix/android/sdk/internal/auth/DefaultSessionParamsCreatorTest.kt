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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.matrix.android.sdk.internal.auth.login.LoginType
import org.matrix.android.sdk.test.fakes.internal.auth.FakeIsValidClientServerApiTask

@ExperimentalCoroutinesApi
class DefaultSessionParamsCreatorTest : DefaultSessionParamsCreatorTestBase() {

    private val fakeIsValidClientServerApiTask = FakeIsValidClientServerApiTask()

    private val sessionParamsCreator = DefaultSessionParamsCreator(fakeIsValidClientServerApiTask.instance)

    @Test
    fun `when create, then SessionParams created`() = runBlockingTest {
        val output = sessionParamsCreator.create(credentials, homeServerConnectionConfig, LoginType.UNKNOWN)

        assertExpectedSessionParams(output)
    }

    @Test
    fun `given credentials contains homeServerUri, when create, then SessionParams created with validated credentials uri`() = runBlockingTest {
        val output = sessionParamsCreator.create(credentialsWithHomeServer, homeServerConnectionConfig, LoginType.UNKNOWN)

        fakeIsValidClientServerApiTask.verifyExecutionWithConfig(homeServerConnectionConfig.copy(homeServerUriBase = discoveryWithHomeServer.getHomeServerUri()))
        assertExpectedSessionParamsWithHomeServer(output)
    }

    @Test
    fun `given credentials homeServerUri is equal to homeServerConnectionConfig, when create, then do not validate`() = runBlockingTest {
        val homeServerConnectionConfigWithCredentialsUri = homeServerConnectionConfig.copy(homeServerUriBase = discoveryWithHomeServer.getHomeServerUri())
        val output = sessionParamsCreator.create(credentialsWithHomeServer, homeServerConnectionConfigWithCredentialsUri, LoginType.UNKNOWN)

        fakeIsValidClientServerApiTask.verifyNoExecution()
        assertExpectedSessionParamsWithHomeServer(output)
    }

    @Test
    fun `given credentials contains identityServerUri, when create, then SessionParams created with credentials uri`() = runBlockingTest {
        val output = sessionParamsCreator.create(credentialsWithIdentityServer, homeServerConnectionConfig, LoginType.UNKNOWN)

        assertExpectedSessionParamsWithIdentityServer(output)
    }
}
