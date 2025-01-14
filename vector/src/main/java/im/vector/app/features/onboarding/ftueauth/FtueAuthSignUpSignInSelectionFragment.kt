/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginSignupSigninSelectionBinding
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.SocialLoginButtonsView.Mode
import im.vector.app.features.login.render
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.SSOAction

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver.
 */
@AndroidEntryPoint
class FtueAuthSignUpSignInSelectionFragment :
        AbstractSSOFtueAuthFragment<FragmentLoginSignupSigninSelectionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginSignupSigninSelectionBinding {
        return FragmentLoginSignupSigninSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        views.loginSignupSigninSubmit.setOnClickListener { submit() }
        views.loginSignupSigninSignIn.setOnClickListener { signIn() }
    }

    private fun render(state: OnboardingViewState) {
        when (state.serverType) {
            ServerType.MatrixOrg -> renderServerInformation(
                    icon = R.drawable.ic_logo_matrix_org,
                    title = getString(CommonStrings.login_connect_to, state.selectedHomeserver.userFacingUrl.toReducedUrl()),
                    subtitle = getString(CommonStrings.login_server_matrix_org_text)
            )
            ServerType.EMS -> renderServerInformation(
                    icon = R.drawable.ic_logo_element_matrix_services,
                    title = getString(CommonStrings.login_connect_to_modular),
                    subtitle = state.selectedHomeserver.userFacingUrl.toReducedUrl()
            )
            ServerType.Other -> renderServerInformation(
                    icon = null,
                    title = getString(CommonStrings.login_server_other_title),
                    subtitle = getString(CommonStrings.login_connect_to, state.selectedHomeserver.userFacingUrl.toReducedUrl())
            )
            ServerType.Unknown -> Unit /* Should not happen */
        }

        when (state.selectedHomeserver.preferredLoginMode) {
            is LoginMode.SsoAndPassword -> {
                views.loginSignupSigninSignInSocialLoginContainer.isVisible = true
                views.loginSignupSigninSocialLoginButtons.render(state.selectedHomeserver.preferredLoginMode, Mode.MODE_CONTINUE) { provider ->
                    viewModel.fetchSsoUrl(
                            redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                            deviceId = state.deviceId,
                            provider = provider,
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

    private fun renderServerInformation(@DrawableRes icon: Int?, title: String, subtitle: String) {
        icon?.let { views.loginSignupSigninServerIcon.setImageResource(it) }
        views.loginSignupSigninServerIcon.isVisible = icon != null
        views.loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
        views.loginSignupSigninTitle.text = title
        views.loginSignupSigninText.text = subtitle
    }

    private fun setupButtons(state: OnboardingViewState) {
        when (state.selectedHomeserver.preferredLoginMode) {
            is LoginMode.Sso -> {
                // change to only one button that is sign in with sso
                views.loginSignupSigninSubmit.text =
                        getString(if (state.selectedHomeserver.hasOidcCompatibilityFlow) CommonStrings.login_continue else CommonStrings.login_signin_sso)
                views.loginSignupSigninSignIn.isVisible = false
            }
            else -> {
                views.loginSignupSigninSubmit.text = getString(CommonStrings.login_signup)
                views.loginSignupSigninSignIn.isVisible = true
            }
        }
    }

    private fun submit() = withState(viewModel) { state ->
        if (state.selectedHomeserver.preferredLoginMode is LoginMode.Sso) {
            viewModel.fetchSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = state.deviceId,
                    provider = null,
                    action = if (state.onboardingFlow == OnboardingFlow.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
            )
                    ?.let { openInCustomTab(it) }
        } else {
            viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignUp))
        }
    }

    private fun signIn() {
        viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignIn))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetSignMode)
    }

    override fun updateWithState(state: OnboardingViewState) {
        render(state)
        setupButtons(state)
        // if talking to OIDC enabled homeserver in compatibility mode then immediately start SSO
        if (state.selectedHomeserver.hasOidcCompatibilityFlow) submit()
    }
}
