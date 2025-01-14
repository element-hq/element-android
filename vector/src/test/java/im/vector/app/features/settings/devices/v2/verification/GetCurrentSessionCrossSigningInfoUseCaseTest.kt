/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
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
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.util.toOptional

private const val A_DEVICE_ID = "device-id"

class GetCurrentSessionCrossSigningInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val getCurrentSessionCrossSigningInfoUseCase = GetCurrentSessionCrossSigningInfoUseCase(
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
    fun `given the active session and existing cross signing info when getting these info then the result is correct`() = runTest(testDispatcher) {
        val fakeSession = givenSession(A_DEVICE_ID)
        val fakeFlowSession = fakeSession.givenFlowSession()
        val isCrossSigningVerified = true
        val mxCrossSigningInfo = givenMxCrossSigningInfo(isCrossSigningVerified)
        every { fakeFlowSession.liveCrossSigningInfo(any()) } returns flowOf(mxCrossSigningInfo.toOptional())
        val expectedResult = CurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = isCrossSigningVerified
        )

        val result = getCurrentSessionCrossSigningInfoUseCase.execute()
                .test(this)

        result.assertValues(listOf(expectedResult))
                .finish()
        verify { fakeFlowSession.liveCrossSigningInfo(fakeSession.myUserId) }
    }

    @Test
    fun `given the active session and no existing cross signing info when getting these info then the result is correct`() = runTest(testDispatcher) {
        val fakeSession = givenSession(A_DEVICE_ID)
        val fakeFlowSession = fakeSession.givenFlowSession()
        val mxCrossSigningInfo = null
        every { fakeFlowSession.liveCrossSigningInfo(any()) } returns flowOf(mxCrossSigningInfo.toOptional())
        val expectedResult = CurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = false,
                isCrossSigningVerified = false
        )

        val result = getCurrentSessionCrossSigningInfoUseCase.execute()
                .test(this)

        result.assertValues(listOf(expectedResult))
                .finish()
        verify { fakeFlowSession.liveCrossSigningInfo(fakeSession.myUserId) }
    }

    @Test
    fun `given no active session when getting cross signing info then the result is empty`() = runTest(testDispatcher) {
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        val result = getCurrentSessionCrossSigningInfoUseCase.execute()
                .test(this)

        result.assertNoValues()
                .finish()
    }

    private fun givenSession(deviceId: String): FakeSession {
        val fakeSession = fakeActiveSessionHolder.fakeSession
        fakeSession.givenSessionId(deviceId)

        return fakeSession
    }

    private fun givenMxCrossSigningInfo(isTrusted: Boolean) = mockk<MXCrossSigningInfo>()
            .also {
                every { it.isTrusted() } returns isTrusted
            }
}
