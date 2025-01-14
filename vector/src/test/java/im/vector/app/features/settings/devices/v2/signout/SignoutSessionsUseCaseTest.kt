/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.signout

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Test

private const val A_DEVICE_ID_1 = "device-id-1"
private const val A_DEVICE_ID_2 = "device-id-2"

class SignoutSessionsUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeInterceptSignoutFlowResponseUseCase = mockk<InterceptSignoutFlowResponseUseCase>()

    private val signoutSessionsUseCase = SignoutSessionsUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            interceptSignoutFlowResponseUseCase = fakeInterceptSignoutFlowResponseUseCase,
    )

    @Test
    fun `given a list of device ids when signing out with success then success result is returned`() = runTest {
        // Given
        val callback = givenOnReAuthCallback()
        val deviceIds = listOf(A_DEVICE_ID_1, A_DEVICE_ID_2)
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDevicesSucceeds(deviceIds)

        // When
        val result = signoutSessionsUseCase.execute(deviceIds, callback)

        // Then
        result.isSuccess shouldBe true
        coVerify {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevices(deviceIds, any())
        }
    }

    @Test
    fun `given a list of device ids when signing out with error then failure result is returned`() = runTest {
        // Given
        val interceptor = givenOnReAuthCallback()
        val deviceIds = listOf(A_DEVICE_ID_1, A_DEVICE_ID_2)
        val error = mockk<Exception>()
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDevicesFailsWithError(deviceIds, error)

        // When
        val result = signoutSessionsUseCase.execute(deviceIds, interceptor)

        // Then
        result.isFailure shouldBe true
        coVerify {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevices(deviceIds, any())
        }
    }

    @Test
    fun `given a list of device ids when signing out with reAuth needed then callback is called`() = runTest {
        // Given
        val callback = givenOnReAuthCallback()
        val deviceIds = listOf(A_DEVICE_ID_1, A_DEVICE_ID_2)
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDevicesNeedsUIAuth(deviceIds)
        val reAuthNeeded = SignoutSessionsReAuthNeeded(
                pendingAuth = mockk(),
                uiaContinuation = mockk(),
                flowResponse = mockk(),
                errCode = "errorCode"
        )
        every { fakeInterceptSignoutFlowResponseUseCase.execute(any(), any(), any()) } returns reAuthNeeded

        // When
        val result = signoutSessionsUseCase.execute(deviceIds, callback)

        // Then
        result.isSuccess shouldBe true
        coVerify {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevices(deviceIds, any())
            callback(reAuthNeeded)
        }
    }

    private fun givenOnReAuthCallback(): (SignoutSessionsReAuthNeeded) -> Unit = {}
}
