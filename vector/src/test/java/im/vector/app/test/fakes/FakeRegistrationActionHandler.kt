/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.onboarding.RegisterAction
import im.vector.app.features.onboarding.RegistrationActionHandler
import io.mockk.coEvery
import io.mockk.mockk

class FakeRegistrationActionHandler {

    val instance = mockk<RegistrationActionHandler>()

    fun givenThrows(action: RegisterAction, cause: Throwable) {
        coEvery { instance.processAction(any(), action) } throws cause
    }

    fun givenResultsFor(result: List<Pair<RegisterAction, RegistrationActionHandler.Result>>) {
        coEvery { instance.processAction(any(), any()) } answers { call ->
            val actionArg = call.invocation.args[1] as RegisterAction
            result.first { it.first == actionArg }.second
        }
    }
}
