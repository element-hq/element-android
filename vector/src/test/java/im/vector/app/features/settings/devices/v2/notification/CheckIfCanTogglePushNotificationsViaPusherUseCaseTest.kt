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

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fixtures.aHomeServerCapabilities
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canRemotelyTogglePushNotificationsOfDevices = true)

class CheckIfCanTogglePushNotificationsViaPusherUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val checkIfCanTogglePushNotificationsViaPusherUseCase =
            CheckIfCanTogglePushNotificationsViaPusherUseCase(
                    activeSessionHolder = fakeActiveSessionHolder.instance,
            )

    @Test
    fun `given current session when execute then toggle capability is returned`() {
        // Given
        fakeActiveSessionHolder
                .fakeSession
                .fakeHomeServerCapabilitiesService
                .givenCapabilities(A_HOMESERVER_CAPABILITIES)

        // When
        val result = checkIfCanTogglePushNotificationsViaPusherUseCase.execute()

        // Then
        result shouldBeEqualTo A_HOMESERVER_CAPABILITIES.canRemotelyTogglePushNotificationsOfDevices
    }

    @Test
    fun `given no current session when execute then false is returned`() {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        // When
        val result = checkIfCanTogglePushNotificationsViaPusherUseCase.execute()

        // Then
        result shouldBeEqualTo false
    }
}
