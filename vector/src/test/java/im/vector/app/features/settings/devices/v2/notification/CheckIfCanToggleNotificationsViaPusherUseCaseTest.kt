/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.aHomeServerCapabilities
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_HOMESERVER_CAPABILITIES = aHomeServerCapabilities(canRemotelyTogglePushNotificationsOfDevices = true)

class CheckIfCanToggleNotificationsViaPusherUseCaseTest {

    private val fakeSession = FakeSession()

    private val checkIfCanToggleNotificationsViaPusherUseCase =
            CheckIfCanToggleNotificationsViaPusherUseCase()

    @Test
    fun `given current session when execute then toggle capability is returned`() {
        // Given
        fakeSession
                .fakeHomeServerCapabilitiesService
                .givenCapabilities(A_HOMESERVER_CAPABILITIES)

        // When
        val result = checkIfCanToggleNotificationsViaPusherUseCase.execute(fakeSession)

        // Then
        result shouldBeEqualTo A_HOMESERVER_CAPABILITIES.canRemotelyTogglePushNotificationsOfDevices
    }
}
