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
