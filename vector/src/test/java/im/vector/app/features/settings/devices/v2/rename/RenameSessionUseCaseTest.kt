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

package im.vector.app.features.settings.devices.v2.rename

import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_DEVICE_ID = "device-id"
private const val A_DEVICE_NAME = "device-name"

class RenameSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val refreshDevicesUseCase = mockk<RefreshDevicesUseCase>()

    private val renameSessionUseCase = RenameSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            refreshDevicesUseCase = refreshDevicesUseCase
    )

    @Test
    fun `given a device id and a new name when no error during rename then the device is renamed with success`() = runTest {
        // Given
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.givenSetDeviceNameSucceeds()
        every { refreshDevicesUseCase.execute() } just runs

        // When
        val result = renameSessionUseCase.execute(A_DEVICE_ID, A_DEVICE_NAME)

        // Then
        result.isSuccess shouldBe true
        verify {
            fakeActiveSessionHolder.fakeSession
                    .cryptoService()
                    .setDeviceName(A_DEVICE_ID, A_DEVICE_NAME, any())
            refreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given a device id and a new name when an error occurs during rename then result is failure`() = runTest {
        // Given
        val error = Exception()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.givenSetDeviceNameFailsWithError(error)

        // When
        val result = renameSessionUseCase.execute(A_DEVICE_ID, A_DEVICE_NAME)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
    }

    @Test
    fun `given a device id and a new name when an error occurs during devices refresh then result is failure`() = runTest {
        // Given
        val error = Exception()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.givenSetDeviceNameSucceeds()
        every { refreshDevicesUseCase.execute() } throws error

        // When
        val result = renameSessionUseCase.execute(A_DEVICE_ID, A_DEVICE_NAME)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
    }
}
