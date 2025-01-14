/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.flow.FlowSession
import org.matrix.android.sdk.flow.flow

class RefreshDevicesOnCryptoDevicesChangeUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val refreshDevicesOnCryptoDevicesChangeUseCase = RefreshDevicesOnCryptoDevicesChangeUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Before
    fun setUp() {
        mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the current session when crypto devices list changes then the devices list is refreshed`() = runTest {
        // Given
        val device1 = givenACryptoDevice()
        val devices = listOf(device1)
        val fakeSession = fakeActiveSessionHolder.fakeSession
        val flowSession = mockk<FlowSession>()
        every { fakeSession.flow() } returns flowSession
        every { flowSession.liveUserCryptoDevices(any()) } returns flowOf(devices)
        coEvery { fakeSession.cryptoService().fetchDevicesList() }

        // When
        refreshDevicesOnCryptoDevicesChangeUseCase.execute()

        // Then
        verify {
            flowSession.liveUserCryptoDevices(fakeSession.myUserId)
            // FIXME the following verification does not work due to the usage of Flow.sample() inside the use case implementation
            // fakeSession.cryptoService().fetchDevicesList(match { it is NoOpMatrixCallback })
        }
    }

    private fun givenACryptoDevice(): CryptoDeviceInfo = mockk()
}
