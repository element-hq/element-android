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

package im.vector.app.features.settings.devices.v2.signout

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor

private const val A_DEVICE_ID_1 = "device-id-1"
private const val A_DEVICE_ID_2 = "device-id-2"

class SignoutSessionsUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val signoutSessionsUseCase = SignoutSessionsUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given a list of device ids when signing out with success then success result is returned`() = runTest {
        // Given
        val interceptor = givenAuthInterceptor()
        val deviceIds = listOf(A_DEVICE_ID_1, A_DEVICE_ID_2)
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDevicesSucceeds(deviceIds)

        // When
        val result = signoutSessionsUseCase.execute(deviceIds, interceptor)

        // Then
        result.isSuccess shouldBe true
        every {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevices(deviceIds, interceptor, any())
        }
    }

    @Test
    fun `given a list of device ids when signing out with error then failure result is returned`() = runTest {
        // Given
        val interceptor = givenAuthInterceptor()
        val deviceIds = listOf(A_DEVICE_ID_1, A_DEVICE_ID_2)
        val error = mockk<Exception>()
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDevicesFailsWithError(deviceIds, error)

        // When
        val result = signoutSessionsUseCase.execute(deviceIds, interceptor)

        // Then
        result.isFailure shouldBe true
        every {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevices(deviceIds, interceptor, any())
        }
    }

    private fun givenAuthInterceptor() = mockk<UserInteractiveAuthInterceptor>()
}
