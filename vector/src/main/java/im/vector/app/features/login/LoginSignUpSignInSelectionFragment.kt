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

import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.toReducedUrl
import kotlinx.android.synthetic.main.fragment_login_signup_signin_selection.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver
 */
class LoginSignUpSignInSelectionFragment @Inject constructor() : AbstractSSOLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_signup_signin_selection

    private fun setupUi(state: LoginViewState) {
        when (state.serverType) {
            ServerType.MatrixOrg -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                loginSignupSigninServerIcon.isVisible = true
                loginSignupSigninTitle.text = getString(R.string.login_connect_to, state.homeServerUrl.toReducedUrl())
                loginSignupSigninText.text = getString(R.string.login_server_matrix_org_text)
            }
            ServerType.EMS -> {
                loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                loginSignupSigninServerIcon.isVisible = true
                loginSignupSigninTitle.text = getString(R.string.login_connect_to_modular)
                loginSignupSigninText.text = state.homeServerUrl.toReducedUrl()
            }
            ServerType.Other -> {
                loginSignupSigninServerIcon.isVisible = false
                loginSignupSigninTitle.text = getString(R.string.login_server_other_title)
                loginSignupSigninText.text = getString(R.string.login_connect_to, state.homeServerUrl.toReducedUrl())
            }
            ServerType.Unknown -> Unit /* Should not happen */
        }

        val identityProviders = state.loginMode.ssoProviders()
        when (state.loginMode) {
            is LoginMode.SsoAndPassword -> {
                loginSignupSigninSignInSocialLoginContainer.isVisible = true
                loginSignupSigninSocialLoginButtons.identityProviders = identityProviders
                loginSignupSigninSocialLoginButtons.listener = object : SocialLoginButtonsView.InteractionListener {
                    override fun onProviderSelected(id: String?) {
                        val url = withState(loginViewModel) { it.getSsoUrl(id) }
                        openInCustomTab(url)
                    }
                }
            }
            else                        -> {
                // SSO only is managed without container as well as No sso
                loginSignupSigninSignInSocialLoginContainer.isVisible = false
                loginSignupSigninSocialLoginButtons.identityProviders = null
            }
        }
    }

    private fun setupButtons(state: LoginViewState) {
        when (state.loginMode) {
            is LoginMode.Sso -> {
                // change to only one button that is sign in with sso
                loginSignupSigninSubmit.text = getString(R.string.login_signin_sso)
                loginSignupSigninSignIn.isVisible = false
            }
            else             -> {
                loginSignupSigninSubmit.text = getString(R.string.login_signup)
                loginSignupSigninSignIn.isVisible = true
            }
        }
    }

    @OnClick(R.id.loginSignupSigninSubmit)
    fun submit() = withState(loginViewModel) { state ->
        if (state.loginMode is LoginMode.Sso) {
            openInCustomTab(state.getSsoUrl(null))
        } else {
            loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignUp))
        }
        Unit
    }

    @OnClick(R.id.loginSignupSigninSignIn)
    fun signIn() {
        loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignIn))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetSignMode)
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)
        setupButtons(state)
    }
}
