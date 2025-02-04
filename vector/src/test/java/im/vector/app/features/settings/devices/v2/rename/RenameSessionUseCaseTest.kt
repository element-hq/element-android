/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.rename

import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
        coEvery { refreshDevicesUseCase.execute() } returns Unit

        // When
        val result = renameSessionUseCase.execute(A_DEVICE_ID, A_DEVICE_NAME)

        // Then
        result.isSuccess shouldBe true
        coVerify {
            fakeActiveSessionHolder.fakeSession
                    .cryptoService()
                    .setDeviceName(A_DEVICE_ID, A_DEVICE_NAME)
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
        coEvery { refreshDevicesUseCase.execute() } throws error

        // When
        val result = renameSessionUseCase.execute(A_DEVICE_ID, A_DEVICE_NAME)

        // Then
        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBeEqualTo error
    }
}
