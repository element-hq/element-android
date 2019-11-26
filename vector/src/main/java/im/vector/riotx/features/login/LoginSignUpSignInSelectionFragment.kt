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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import kotlinx.android.synthetic.main.fragment_login_signup_signin_selection.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver
 */
class LoginSignUpSignInSelectionFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_signup_signin_selection

    private var isSsoSignIn: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isSsoSignIn = withState(loginViewModel) { it.asyncHomeServerLoginFlowRequest.invoke() } == LoginMode.Sso

        setupUi()
        setupButtons()
    }

    private fun setupUi() {
        when (loginViewModel.serverType) {
            ServerType.MatrixOrg -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                loginSignupSigninServerIcon.isVisible = true
                loginSignupSigninTitle.text = getString(R.string.login_connect_to, loginViewModel.getHomeServerUrlSimple())
                loginSignupSigninText.text = getString(R.string.login_server_matrix_org_text)
            }
            ServerType.Modular   -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_modular)
                loginSignupSigninServerIcon.isVisible = true
                // TODO
                loginSignupSigninTitle.text = getString(R.string.login_connect_to, "TODO MODULAR NAME")
                loginSignupSigninText.text = loginViewModel.getHomeServerUrlSimple()
            }
            ServerType.Other     -> {
                loginSignupSigninServerIcon.isVisible = false
                loginSignupSigninTitle.text = getString(R.string.login_server_other_title)
                loginSignupSigninText.text = getString(R.string.login_connect_to, loginViewModel.getHomeServerUrlSimple())
            }
        }
    }

    private fun setupButtons() {
        if (isSsoSignIn) {
            loginSignupSigninSubmit.text = getString(R.string.login_signin_sso)
            loginSignupSigninSignIn.isVisible = false
        } else {
            loginSignupSigninSubmit.text = getString(R.string.login_signup)
            loginSignupSigninSignIn.isVisible = true
        }
    }

    @OnClick(R.id.loginSignupSigninSubmit)
    fun signUp() {
        if (isSsoSignIn) {
            signIn()
        } else {
            loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignUp))
        }
    }

    @OnClick(R.id.loginSignupSigninSignIn)
    fun signIn() {
        loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignIn))
        loginSharedActionViewModel.post(LoginNavigation.OnSignModeSelected)
    }

    override fun onError(throwable: Throwable) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetSignMode)
    }
}
