/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeVectorPreferences
import org.junit.Test

class ToggleIpAddressVisibilityUseCaseTest {

    private val fakeVectorPreferences = FakeVectorPreferences()

    private val toggleIpAddressVisibilityUseCase = ToggleIpAddressVisibilityUseCase(
            vectorPreferences = fakeVectorPreferences.instance,
    )

    @Test
    fun `given ip addresses are currently visible then then visibility is set as false`() {
        // Given
        fakeVectorPreferences.givenShowIpAddressInSessionManagerScreens(true)

        // When
        toggleIpAddressVisibilityUseCase.execute()

        // Then
        fakeVectorPreferences.verifySetIpAddressVisibilityInDeviceManagerScreens(false)
    }

    @Test
    fun `given ip addresses are currently not visible then then visibility is set as true`() {
        // Given
        fakeVectorPreferences.givenShowIpAddressInSessionManagerScreens(false)

        // When
        toggleIpAddressVisibilityUseCase.execute()

        // Then
        fakeVectorPreferences.verifySetIpAddressVisibilityInDeviceManagerScreens(true)
    }
}
