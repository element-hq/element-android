/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.settings.devices.v2.signout.SignoutSessionsReAuthNeeded
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse

class FakeSignoutSessionsUseCase {

    val instance = mockk<SignoutSessionsUseCase>()

    fun givenSignoutSuccess(deviceIds: List<String>) {
        coEvery { instance.execute(deviceIds, any()) } returns Result.success(Unit)
    }

    fun givenSignoutReAuthNeeded(deviceIds: List<String>): SignoutSessionsReAuthNeeded {
        val flowResponse = mockk<RegistrationFlowResponse>()
        every { flowResponse.session } returns "a-session-id"
        val errorCode = "errorCode"
        val reAuthNeeded = SignoutSessionsReAuthNeeded(
                pendingAuth = mockk(),
                uiaContinuation = mockk(),
                flowResponse = flowResponse,
                errCode = errorCode,
        )
        coEvery { instance.execute(deviceIds, any()) } coAnswers {
            secondArg<(SignoutSessionsReAuthNeeded) -> Unit>().invoke(reAuthNeeded)
            Result.success(Unit)
        }

        return reAuthNeeded
    }

    fun givenSignoutError(deviceIds: List<String>, error: Throwable) {
        coEvery { instance.execute(deviceIds, any()) } returns Result.failure(error)
    }
}
