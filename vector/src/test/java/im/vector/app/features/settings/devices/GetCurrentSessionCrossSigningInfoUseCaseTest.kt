/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices

import im.vector.app.features.settings.devices.v2.CurrentSessionCrossSigningInfo
import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.SessionParams

private const val A_DEVICE_ID = "device-id"

class GetCurrentSessionCrossSigningInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val getCurrentSessionCrossSigningInfoUseCase = GetCurrentSessionCrossSigningInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given the active session when getting cross signing info then the result is correct`() {
        val sessionParams = mockk<SessionParams>()
        every { sessionParams.deviceId } returns A_DEVICE_ID
        fakeActiveSessionHolder.fakeSession.givenSessionParams(sessionParams)
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

        val result = getCurrentSessionCrossSigningInfoUseCase.execute()

        result shouldBeEqualTo expectedResult
    }
}
