/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.SSOAction
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

    fun givenSsoUrl(redirectUri: String, deviceId: String, providerId: String, action: SSOAction, result: String) {
        coEvery { getSsoUrl(redirectUri, deviceId, providerId, action) } returns result
    }
}
