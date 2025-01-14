/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginSignupSigninSelectionBinding
import im.vector.app.features.login.SocialLoginButtonsView.Mode
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.SSOAction

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver.
 */
@AndroidEntryPoint
class LoginSignUpSignInSelectionFragment :
        AbstractSSOLoginFragment<FragmentLoginSignupSigninSelectionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginSignupSigninSelectionBinding {
        return FragmentLoginSignupSigninSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        views.loginSignupSigninSubmit.debouncedClicks { submit() }
        views.loginSignupSigninSignIn.debouncedClicks { signIn() }
    }

    private fun setupUi(state: LoginViewState) {
        when (state.serverType) {
            ServerType.MatrixOrg -> {
                views.loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                views.loginSignupSigninServerIcon.isVisible = true
                views.loginSignupSigninTitle.text = getString(CommonStrings.login_connect_to, state.homeServerUrlFromUser.toReducedUrl())
                views.loginSignupSigninText.text = getString(CommonStrings.login_server_matrix_org_text)
            }
            ServerType.EMS -> {
                views.loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                views.loginSignupSigninServerIcon.isVisible = true
                views.loginSignupSigninTitle.text = getString(CommonStrings.login_connect_to_modular)
                views.loginSignupSigninText.text = state.homeServerUrlFromUser.toReducedUrl()
            }
            ServerType.Other -> {
                views.loginSignupSigninServerIcon.isVisible = false
                views.loginSignupSigninTitle.text = getString(CommonStrings.login_server_other_title)
                views.loginSignupSigninText.text = getString(CommonStrings.login_connect_to, state.homeServerUrlFromUser.toReducedUrl())
            }
            ServerType.Unknown -> Unit /* Should not happen */
        }

        when (state.loginMode) {
            is LoginMode.SsoAndPassword -> {
                views.loginSignupSigninSignInSocialLoginContainer.isVisible = true
                views.loginSignupSigninSocialLoginButtons.render(state.loginMode, Mode.MODE_CONTINUE) { provider ->
                    loginViewModel.getSsoUrl(
                            redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                            deviceId = state.deviceId,
                            providerId = provider?.id,
                            action = if (state.signMode == SignMode.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
                    )
                            ?.let { openInCustomTab(it) }
                }
            }
            else -> {
                // SSO only is managed without container as well as No sso
                views.loginSignupSigninSignInSocialLoginContainer.isVisible = false
                views.loginSignupSigninSocialLoginButtons.ssoIdentityProviders = null
            }
        }
    }

    private fun setupButtons(state: LoginViewState) {
        when (state.loginMode) {
            is LoginMode.Sso -> {
                // change to only one button that is sign in with sso
                views.loginSignupSigninSubmit.text = getString(CommonStrings.login_signin_sso)
                views.loginSignupSigninSignIn.isVisible = false
            }
            else -> {
                views.loginSignupSigninSubmit.text = getString(CommonStrings.login_signup)
                views.loginSignupSigninSignIn.isVisible = true
            }
        }
    }

    private fun submit() = withState(loginViewModel) { state ->
        if (state.loginMode is LoginMode.Sso) {
            loginViewModel.getSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = state.deviceId,
                    providerId = null,
                    action = if (state.signMode == SignMode.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
            )
                    ?.let { openInCustomTab(it) }
        } else {
            loginViewModel.handle(LoginAction.UpdateSignMode(SignMode.SignUp))
        }
    }

    private fun signIn() {
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
