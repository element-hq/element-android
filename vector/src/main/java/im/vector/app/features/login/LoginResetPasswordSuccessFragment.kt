/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.login

import butterknife.OnClick
import im.vector.app.R
import javax.inject.Inject

/**
 * In this screen, we confirm to the user that his password has been reset
 */
class LoginResetPasswordSuccessFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_reset_password_success

    @OnClick(R.id.resetPasswordSuccessSubmit)
    fun submit() {
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnResetPasswordMailConfirmationSuccessDone))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }
}
