/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.autofill.HintConstants
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginBinding
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.SocialLoginButtonsView.Mode
import im.vector.app.features.login.render
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.auth.SSOAction
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isInvalidUsername
import org.matrix.android.sdk.api.failure.isLoginEmailUnknown
import org.matrix.android.sdk.api.failure.isRegistrationDisabled
import org.matrix.android.sdk.api.failure.isUsernameInUse
import org.matrix.android.sdk.api.failure.isWeakPassword
import reactivecircus.flowbinding.android.widget.textChanges

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
@AndroidEntryPoint
class FtueAuthLoginFragment :
        AbstractSSOFtueAuthFragment<FragmentLoginBinding>() {

    private var isSignupMode = false

    // Temporary patch for https://github.com/element-hq/riotX-android/issues/1410,
    // waiting for https://github.com/matrix-org/synapse/issues/7576
    private var isNumericOnlyUserIdForbidden = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupForgottenPasswordButton()

        views.passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupForgottenPasswordButton() {
        views.forgetPasswordButton.setOnClickListener { forgetPasswordClicked() }
    }

    private fun setupAutoFill(state: OnboardingViewState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> {
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                }
                SignMode.SignIn,
                SignMode.SignInWithMatrixId -> {
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
                }
            }
        }
    }

    private fun ssoMode(state: OnboardingViewState) = when (state.signMode) {
        SignMode.Unknown -> error("developer error")
        SignMode.SignUp -> Mode.MODE_SIGN_UP
        SignMode.SignIn,
        SignMode.SignInWithMatrixId -> Mode.MODE_SIGN_IN
    }

    private fun submit() {
        withState(viewModel) { state ->
            cleanupUi()

            val login = views.loginField.text.toString()
            val password = views.passwordField.text.toString()

            // This can be called by the IME action, so deal with empty cases
            var error = 0
            if (login.isEmpty()) {
                views.loginFieldTil.error = getString(
                        if (isSignupMode) {
                            CommonStrings.error_empty_field_choose_user_name
                        } else {
                            CommonStrings.error_empty_field_enter_user_name
                        }
                )
                error++
            }
            if (isSignupMode && isNumericOnlyUserIdForbidden && login.isDigitsOnly()) {
                views.loginFieldTil.error = getString(CommonStrings.error_forbidden_digits_only_username)
                error++
            }
            if (password.isEmpty()) {
                views.passwordFieldTil.error = getString(
                        if (isSignupMode) {
                            CommonStrings.error_empty_field_choose_password
                        } else {
                            CommonStrings.error_empty_field_your_password
                        }
                )
                error++
            }

            if (error == 0) {
                val initialDeviceName = getString(CommonStrings.login_default_session_public_name)
                viewModel.handle(state.signMode.toAuthenticateAction(login, password, initialDeviceName))
            }
        }
    }

    private fun cleanupUi() {
        views.loginSubmit.hideKeyboard()
        views.loginFieldTil.error = null
        views.passwordFieldTil.error = null
    }

    private fun setupUi(state: OnboardingViewState) {
        views.loginFieldTil.hint = getString(
                when (state.signMode) {
                    SignMode.Unknown -> error("developer error")
                    SignMode.SignUp -> CommonStrings.login_signup_username_hint
                    SignMode.SignIn -> CommonStrings.login_signin_username_hint
                    SignMode.SignInWithMatrixId -> CommonStrings.login_signin_matrix_id_hint
                }
        )

        // Handle direct signin first
        if (state.signMode == SignMode.SignInWithMatrixId) {
            views.loginServerIcon.isVisible = false
            views.loginTitle.text = getString(CommonStrings.login_signin_matrix_id_title)
            views.loginNotice.text = getString(CommonStrings.login_signin_matrix_id_notice)
            views.loginPasswordNotice.isVisible = true
        } else {
            val resId = when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> CommonStrings.login_signup_to
                SignMode.SignIn -> CommonStrings.login_connect_to
                SignMode.SignInWithMatrixId -> CommonStrings.login_connect_to
            }

            when (state.serverType) {
                ServerType.MatrixOrg -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                    views.loginTitle.text = getString(resId, state.selectedHomeserver.userFacingUrl.toReducedUrl())
                    views.loginNotice.text = getString(CommonStrings.login_server_matrix_org_text)
                }
                ServerType.EMS -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                    views.loginTitle.text = getString(resId, "Element Matrix Services")
                    views.loginNotice.text = getString(CommonStrings.login_server_modular_text)
                }
                ServerType.Other -> {
                    views.loginServerIcon.isVisible = false
                    views.loginTitle.text = getString(resId, state.selectedHomeserver.userFacingUrl.toReducedUrl())
                    views.loginNotice.text = getString(CommonStrings.login_server_other_text)
                }
                ServerType.Unknown -> Unit /* Should not happen */
            }
            views.loginPasswordNotice.isVisible = false

            if (state.selectedHomeserver.preferredLoginMode is LoginMode.SsoAndPassword) {
                views.loginSocialLoginContainer.isVisible = true
                views.loginSocialLoginButtons.render(state.selectedHomeserver.preferredLoginMode, ssoMode(state)) { provider ->
                    viewModel.fetchSsoUrl(
                            redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                            deviceId = state.deviceId,
                            provider = provider,
                            action = if (state.signMode == SignMode.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
                    )
                            ?.let { openInCustomTab(it) }
                }
            } else {
                views.loginSocialLoginContainer.isVisible = false
                views.loginSocialLoginButtons.ssoIdentityProviders = null
            }
        }
    }

    private fun setupButtons(state: OnboardingViewState) {
        views.forgetPasswordButton.isVisible = state.signMode == SignMode.SignIn

        views.loginSubmit.text = getString(
                when (state.signMode) {
                    SignMode.Unknown -> error("developer error")
                    SignMode.SignUp -> CommonStrings.login_signup_submit
                    SignMode.SignIn,
                    SignMode.SignInWithMatrixId -> CommonStrings.login_signin
                }
        )
    }

    private fun setupSubmitButton() {
        views.loginSubmit.setOnClickListener { submit() }
        combine(
                views.loginField.textChanges().map { it.trim().isNotEmpty() },
                views.passwordField.textChanges().map { it.isNotEmpty() }
        ) { isLoginNotEmpty, isPasswordNotEmpty ->
            isLoginNotEmpty && isPasswordNotEmpty
        }
                .onEach {
                    views.loginFieldTil.error = null
                    views.passwordFieldTil.error = null
                    views.loginSubmit.isEnabled = it
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun forgetPasswordClicked() {
        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnForgetPasswordClicked))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun onError(throwable: Throwable) {
        // Trick to display the error without text.
        views.loginFieldTil.error = " "
        when {
            throwable.isUsernameInUse() || throwable.isInvalidUsername() -> {
                views.loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isLoginEmailUnknown() -> {
                views.loginFieldTil.error = getString(CommonStrings.login_login_with_email_error)
            }
            throwable.isInvalidPassword() && spaceInPassword() -> {
                views.passwordFieldTil.error = getString(CommonStrings.auth_invalid_login_param_space_in_password)
            }
            throwable.isWeakPassword() || throwable.isInvalidPassword() -> {
                views.passwordFieldTil.error = errorFormatter.toHumanReadable(throwable)
            }
            isSignupMode && throwable.isRegistrationDisabled() -> {
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(CommonStrings.dialog_title_error)
                        .setMessage(getString(CommonStrings.login_registration_disabled))
                        .setPositiveButton(CommonStrings.ok, null)
                        .show()
            }
            else -> {
                super.onError(throwable)
            }
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        isSignupMode = state.signMode == SignMode.SignUp
        isNumericOnlyUserIdForbidden = state.serverType == ServerType.MatrixOrg

        setupUi(state)
        setupAutoFill(state)
        setupButtons(state)

        if (state.isLoading) {
            // Ensure password is hidden
            views.passwordField.hidePassword()
        }
    }

    /**
     * Detect if password ends or starts with spaces.
     */
    private fun spaceInPassword() = views.passwordField.text.toString().let { it.trim() != it }
}
