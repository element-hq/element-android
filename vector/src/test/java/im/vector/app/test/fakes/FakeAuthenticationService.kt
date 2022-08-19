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

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult

class FakeAuthenticationService : AuthenticationService by mockk() {

    fun givenRegistrationWizard(registrationWizard: RegistrationWizard) {
        every { getRegistrationWizard() } returns registrationWizard
    }

    fun givenRegistrationStarted(started: Boolean) {
        every { isRegistrationStarted() } returns started
    }

    fun givenLoginWizard(loginWizard: LoginWizard) {
        every { getLoginWizard() } returns loginWizard
    }

    fun givenLoginFlow(config: HomeServerConnectionConfig, result: LoginFlowResult) {
        coEvery { getLoginFlow(config) } returns result
    }

    fun expectReset() {
        coJustRun { reset() }
    }

    fun expectedCancelsPendingLogin() {
        coJustRun { cancelPendingLoginOrRegistration() }
    }

    fun givenWellKnown(matrixId: String, config: HomeServerConnectionConfig?, result: WellknownResult) {
        coEvery { getWellKnownData(matrixId, config) } returns result
    }

    fun givenWellKnownThrows(matrixId: String, config: HomeServerConnectionConfig?, cause: Throwable) {
        coEvery { getWellKnownData(matrixId, config) } throws cause
    }

    fun givenDirectAuthentication(config: HomeServerConnectionConfig, matrixId: String, password: String, deviceName: String, result: FakeSession) {
        coEvery { directAuthentication(config, matrixId, password, deviceName) } returns result
    }

    fun givenDirectAuthenticationThrows(config: HomeServerConnectionConfig, matrixId: String, password: String, deviceName: String, cause: Throwable) {
        coEvery { directAuthentication(config, matrixId, password, deviceName) } throws cause
    }

    fun verifyReset() {
        coVerify { reset() }
    }

    fun verifyCancelsPendingLogin() {
        coVerify { cancelPendingLoginOrRegistration() }
    }

    fun givenSsoUrl(redirectUri: String, deviceId: String, providerId: String, result: String) {
        coEvery { getSsoUrl(redirectUri, deviceId, providerId) } returns result
    }
}
