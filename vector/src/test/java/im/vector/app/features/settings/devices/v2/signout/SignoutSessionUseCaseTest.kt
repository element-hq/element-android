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

private const val A_DEVICE_ID = "device-id"

class SignoutSessionUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val signoutSessionUseCase = SignoutSessionUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given a device id when signing out with success then success result is returned`() = runTest {
        // Given
        val interceptor = givenAuthInterceptor()
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDeviceSucceeds(A_DEVICE_ID)

        // When
        val result = signoutSessionUseCase.execute(A_DEVICE_ID, interceptor)

        // Then
        result.isSuccess shouldBe true
        every {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevice(A_DEVICE_ID, interceptor, any())
        }
    }

    @Test
    fun `given a device id when signing out with error then failure result is returned`() = runTest {
        // Given
        val interceptor = givenAuthInterceptor()
        val error = mockk<Exception>()
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .givenDeleteDeviceFailsWithError(A_DEVICE_ID, error)

        // When
        val result = signoutSessionUseCase.execute(A_DEVICE_ID, interceptor)

        // Then
        result.isFailure shouldBe true
        every {
            fakeActiveSessionHolder.fakeSession
                    .fakeCryptoService
                    .deleteDevice(A_DEVICE_ID, interceptor, any())
        }
    }

    private fun givenAuthInterceptor() = mockk<UserInteractiveAuthInterceptor>()
}
