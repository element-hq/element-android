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

package im.vector.riotx.features.login

import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import kotlinx.android.synthetic.main.fragment_login_signup_signin_selection.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver
 */
class LoginSignUpSignInSelectionFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_signup_signin_selection

    private fun updateViews(serverType: ServerType) {
        when (serverType) {
            ServerType.MatrixOrg -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                loginSignupSigninServerIcon.isVisible = true
                loginSignupSigninTitle.text = getString(R.string.login_connect_to, "matrix.org")
                loginSignupSigninText.text = getString(R.string.login_server_matrix_org_text)
            }
            ServerType.Modular   -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_modular)
                loginSignupSigninServerIcon.isVisible = true
                loginSignupSigninTitle.text = getString(R.string.login_connect_to, "TODO MODULAR NAME")
                loginSignupSigninText.text = "TODO MODULAR URL"
            }
            ServerType.Other     -> {
                loginSignupSigninServerIcon.isVisible = false
                loginSignupSigninTitle.text = getString(R.string.login_server_other_title)
                loginSignupSigninText.text = "TODO SERVER URL"
            }
        }
    }

    @OnClick(R.id.loginSignupSigninSignUp)
    fun signUp() {
        viewModel.handle(LoginAction.UpdateSignMode(SignMode.SignUp))
        loginSharedActionViewModel.post(LoginNavigation.OnSignModeSelected)
    }

    @OnClick(R.id.loginSignupSigninSignIn)
    fun signIn() {
        viewModel.handle(LoginAction.UpdateSignMode(SignMode.SignIn))
        loginSharedActionViewModel.post(LoginNavigation.OnSignModeSelected)
    }

    override fun resetViewModel() {
        viewModel.handle(LoginAction.ResetSignMode)
    }

    override fun invalidate() = withState(viewModel) {
        updateViews(it.serverType)
    }
}
