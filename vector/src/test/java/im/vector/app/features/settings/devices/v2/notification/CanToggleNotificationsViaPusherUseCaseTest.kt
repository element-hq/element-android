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

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.givenAsFlow
import im.vector.app.test.fixtures.aHomeServerCapabilities
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test

private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canRemotelyTogglePushNotificationsOfDevices = true)

class CanToggleNotificationsViaPusherUseCaseTest {

    private val fakeSession = FakeSession()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val canToggleNotificationsViaPusherUseCase =
            CanToggleNotificationsViaPusherUseCase()

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given current session when execute then flow of the toggle capability is returned`() = runTest {
        // Given
        fakeSession
                .fakeHomeServerCapabilitiesService
                .givenCapabilitiesLiveReturns(A_HOMESERVER_CAPABILITIES)
                .givenAsFlow()

        // When
        val result = canToggleNotificationsViaPusherUseCase.execute(fakeSession).firstOrNull()

        // Then
        result shouldBeEqualTo A_HOMESERVER_CAPABILITIES.canRemotelyTogglePushNotificationsOfDevices
    }
}
