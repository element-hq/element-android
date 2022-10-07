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

package im.vector.app.features.settings.devices.v2.verification

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.flow.FlowSession
import org.matrix.android.sdk.flow.flow

class CheckIfCurrentSessionCanBeVerifiedUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val checkIfCurrentSessionCanBeVerifiedUseCase = CheckIfCurrentSessionCanBeVerifiedUseCase(
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
    fun `given there are other sessions when checking if session can be verified then result is true`() = runTest {
        // Given
        val device1 = givenACryptoDevice()
        val device2 = givenACryptoDevice()
        val devices = listOf(device1, device2)
        val fakeSession = fakeActiveSessionHolder.fakeSession
        val flowSession = mockk<FlowSession>()
        every { fakeSession.flow() } returns flowSession
        every { flowSession.liveUserCryptoDevices(any()) } returns flowOf(devices)

        fakeSession.fakeSharedSecretStorageService.givenIsRecoverySetupReturns(false)

        // When
        val result = checkIfCurrentSessionCanBeVerifiedUseCase.execute()

        // Then
        result shouldBeEqualTo true
        verify {
            flowSession.liveUserCryptoDevices(fakeSession.myUserId)
            fakeSession.fakeSharedSecretStorageService.isRecoverySetup()
        }
    }

    @Test
    fun `given recovery is setup when checking if session can be verified then result is true`() = runTest {
        // Given
        val device1 = givenACryptoDevice()
        val devices = listOf(device1)
        val fakeSession = fakeActiveSessionHolder.fakeSession
        val flowSession = mockk<FlowSession>()
        every { fakeSession.flow() } returns flowSession
        every { flowSession.liveUserCryptoDevices(any()) } returns flowOf(devices)

        fakeSession.fakeSharedSecretStorageService.givenIsRecoverySetupReturns(true)

        // When
        val result = checkIfCurrentSessionCanBeVerifiedUseCase.execute()

        // Then
        result shouldBeEqualTo true
        verify {
            flowSession.liveUserCryptoDevices(fakeSession.myUserId)
            fakeSession.fakeSharedSecretStorageService.isRecoverySetup()
        }
    }

    @Test
    fun `given recovery is not setup and there are no other sessions when checking if session can be verified then result is false`() = runTest {
        // Given
        val device1 = givenACryptoDevice()
        val devices = listOf(device1)
        val fakeSession = fakeActiveSessionHolder.fakeSession
        val flowSession = mockk<FlowSession>()
        every { fakeSession.flow() } returns flowSession
        every { flowSession.liveUserCryptoDevices(any()) } returns flowOf(devices)

        fakeSession.fakeSharedSecretStorageService.givenIsRecoverySetupReturns(false)

        // When
        val result = checkIfCurrentSessionCanBeVerifiedUseCase.execute()

        // Then
        result shouldBeEqualTo false
        verify {
            flowSession.liveUserCryptoDevices(fakeSession.myUserId)
            fakeSession.fakeSharedSecretStorageService.isRecoverySetup()
        }
    }

    private fun givenACryptoDevice(): CryptoDeviceInfo = mockk()
}
