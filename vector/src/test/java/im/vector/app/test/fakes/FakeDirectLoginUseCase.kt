/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.onboarding.DirectLoginUseCase
import im.vector.app.features.onboarding.OnboardingAction.AuthenticateAction
import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig

class FakeDirectLoginUseCase {
    val instance = mockk<DirectLoginUseCase>()

    fun givenSuccessResult(action: AuthenticateAction.LoginDirect, config: HomeServerConnectionConfig?, result: FakeSession) {
        coEvery { instance.execute(action, config) } returns Result.success(result)
    }

    fun givenFailureResult(action: AuthenticateAction.LoginDirect, config: HomeServerConnectionConfig?, cause: Throwable) {
        coEvery { instance.execute(action, config) } returns Result.failure(cause)
    }
}
