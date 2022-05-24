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

package im.vector.app.test.fakes

import im.vector.app.features.onboarding.RegisterAction
import im.vector.app.features.onboarding.RegistrationActionHandler
import im.vector.app.features.onboarding.RegistrationResult
import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard

class FakeRegisterActionHandler {

    val instance = mockk<RegistrationActionHandler>()

    fun givenResultsFor(wizard: RegistrationWizard, result: List<Pair<RegisterAction, RegistrationResult>>) {
        coEvery { instance.handleRegisterAction(wizard, any()) } answers { call ->
            val actionArg = call.invocation.args[1] as RegisterAction
            result.first { it.first == actionArg }.second
        }
    }

    fun givenThrowsFor(wizard: RegistrationWizard, action: RegisterAction, cause: Throwable) {
        coEvery { instance.handleRegisterAction(wizard, action) } throws cause
    }
}
