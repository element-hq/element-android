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
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.session.Session

class FakeLoginWizard : LoginWizard by mockk() {

    fun givenResetPasswordSuccess(email: String) {
        coJustRun { resetPassword(email) }
    }

    fun givenLoginWithTokenResult(token: String, result: Session) {
        coEvery { loginWithToken(token) } returns result
    }

    fun givenLoginSuccess(username: String, password: String, deviceName: String, result: Session) {
        coEvery { login(username, password, deviceName) } returns result
    }

    fun givenConfirmResetPasswordSuccess(password: String) {
        coJustRun { resetPasswordMailConfirmed(password) }
    }

    fun verifyResetPassword(email: String) {
        coVerify { resetPassword(email) }
    }
}
