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

package im.vector.app.features.settings.devices

import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import im.vector.app.test.fakes.FakeActiveSessionHolder
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_DEVICE_ID = "device-id"

class GetCurrentSessionCrossSigningInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val getCurrentSessionCrossSigningInfoUseCase = GetCurrentSessionCrossSigningInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given the active session when getting cross signing info then the result is correct`() {
        fakeActiveSessionHolder.fakeSession.givenSessionId(A_DEVICE_ID)
        val isCrossSigningInitialized = true
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .fakeCrossSigningService
                .givenIsCrossSigningInitializedReturns(isCrossSigningInitialized)
        val isCrossSigningVerified = true
        fakeActiveSessionHolder.fakeSession
                .fakeCryptoService
                .fakeCrossSigningService
                .givenIsCrossSigningVerifiedReturns(isCrossSigningVerified)
        val expectedResult = CurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = isCrossSigningInitialized,
                isCrossSigningVerified = isCrossSigningVerified
        )

        val result = runBlocking {
            getCurrentSessionCrossSigningInfoUseCase.execute()
        }

        result shouldBeEqualTo expectedResult
    }
}
