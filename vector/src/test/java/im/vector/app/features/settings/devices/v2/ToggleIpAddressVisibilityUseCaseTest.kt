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
