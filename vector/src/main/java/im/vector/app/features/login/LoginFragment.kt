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

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.autofill.HintConstants
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.extensions.toReducedUrl
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login_signup_signin_selection.*
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.isInvalidPassword
import javax.inject.Inject

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class LoginFragment @Inject constructor() : AbstractSSOLoginFragment() {

    private var passwordShown = false
    private var isSignupMode = false

    // Temporary patch for https://github.com/vector-im/riotX-android/issues/1410,
    // waiting for https://github.com/matrix-org/synapse/issues/7576
    private var isNumericOnlyUserIdForbidden = false

    override fun getLayoutResId() = R.layout.fragment_login

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupPasswordReveal()

        passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupAutoFill(state: LoginViewState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> {
                    loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
                    passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                    loginSocialLoginButtons.mode = SocialLoginButtonsView.Mode.MODE_SIGN_UP
                }
                SignMode.SignIn,
                SignMode.SignInWithMatrixId -> {
                    loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_USERNAME)
                    passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
                    loginSocialLoginButtons.mode = SocialLoginButtonsView.Mode.MODE_SIGN_IN
                }
            }.exhaustive
        }
    }

    @OnClick(R.id.loginSubmit)
    fun submit() {
        cleanupUi()

        val login = loginField.text.toString()
        val password = passwordField.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty()) {
            loginFieldTil.error = getString(if (isSignupMode) R.string.error_empty_field_choose_user_name else R.string.error_empty_field_enter_user_name)
            error++
        }
        if (isSignupMode && isNumericOnlyUserIdForbidden && login.isDigitsOnly()) {
            loginFieldTil.error = "The homeserver does not accept username with only digits."
            error++
        }
        if (password.isEmpty()) {
            passwordFieldTil.error = getString(if (isSignupMode) R.string.error_empty_field_choose_password else R.string.error_empty_field_your_password)
            error++
        }

        if (error == 0) {
            loginViewModel.handle(LoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
        }
    }

    private fun cleanupUi() {
        loginSubmit.hideKeyboard()
        loginFieldTil.error = null
        passwordFieldTil.error = null
    }

    private fun setupUi(state: LoginViewState) {
        loginFieldTil.hint = getString(when (state.signMode) {
            SignMode.Unknown            -> error("developer error")
            SignMode.SignUp             -> R.string.login_signup_username_hint
            SignMode.SignIn             -> R.string.login_signin_username_hint
            SignMode.SignInWithMatrixId -> R.string.login_signin_matrix_id_hint
        })

        // Handle direct signin first
        if (state.signMode == SignMode.SignInWithMatrixId) {
            loginServerIcon.isVisible = false
            loginTitle.text = getString(R.string.login_signin_matrix_id_title)
            loginNotice.text = getString(R.string.login_signin_matrix_id_notice)
            loginPasswordNotice.isVisible = true
        } else {
            val resId = when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> R.string.login_signup_to
                SignMode.SignIn -> R.string.login_connect_to
                SignMode.SignInWithMatrixId -> R.string.login_connect_to
            }

            when (state.serverType) {
                ServerType.MatrixOrg -> {
                    loginServerIcon.isVisible = true
                    loginServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                    loginTitle.text = getString(resId, state.homeServerUrl.toReducedUrl())
                    loginNotice.text = getString(R.string.login_server_matrix_org_text)
                }
                ServerType.EMS -> {
                    loginServerIcon.isVisible = true
                    loginServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                    loginTitle.text = getString(resId, "Element Matrix Services")
                    loginNotice.text = getString(R.string.login_server_modular_text)
                }
                ServerType.Other -> {
                    loginServerIcon.isVisible = false
                    loginTitle.text = getString(resId, state.homeServerUrl.toReducedUrl())
                    loginNotice.text = getString(R.string.login_server_other_text)
                }
                ServerType.Unknown -> Unit /* Should not happen */
            }
            loginPasswordNotice.isVisible = false

            if (state.loginMode is LoginMode.SsoAndPassword) {
                loginSocialLoginContainer.isVisible = true
                loginSocialLoginButtons.identityProviders = state.loginMode.identityProviders
                loginSocialLoginButtons.listener = object : SocialLoginButtonsView.InteractionListener {
                    override fun onProviderSelected(id: String?) {
                        openInCustomTab(state.getSsoUrl(id))
                    }
                }
            } else {
                loginSocialLoginContainer.isVisible = false
                loginSocialLoginButtons.identityProviders = null
            }
        }
    }

    private fun setupButtons(state: LoginViewState) {
        forgetPasswordButton.isVisible = state.signMode == SignMode.SignIn

        loginSubmit.text = getString(when (state.signMode) {
            SignMode.Unknown            -> error("developer error")
            SignMode.SignUp             -> R.string.login_signup_submit
            SignMode.SignIn,
            SignMode.SignInWithMatrixId -> R.string.login_signin
        })
    }

    private fun setupSubmitButton() {
        Observable
                .combineLatest(
                        loginField.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.isNotEmpty() },
                        BiFunction<Boolean, Boolean, Boolean> { isLoginNotEmpty, isPasswordNotEmpty ->
                            isLoginNotEmpty && isPasswordNotEmpty
                        }
                )
                .subscribeBy {
                    loginFieldTil.error = null
                    passwordFieldTil.error = null
                    loginSubmit.isEnabled = it
                }
                .disposeOnDestroyView()
    }

    @OnClick(R.id.forgetPasswordButton)
    fun forgetPasswordClicked() {
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnForgetPasswordClicked))
    }

    private fun setupPasswordReveal() {
        passwordShown = false

        passwordReveal.setOnClickListener {
            passwordShown = !passwordShown

            renderPasswordField()
        }

        renderPasswordField()
    }

    private fun renderPasswordField() {
        passwordField.showPassword(passwordShown)

        if (passwordShown) {
            passwordReveal.setImageResource(R.drawable.ic_eye_closed)
            passwordReveal.contentDescription = getString(R.string.a11y_hide_password)
        } else {
            passwordReveal.setImageResource(R.drawable.ic_eye)
            passwordReveal.contentDescription = getString(R.string.a11y_show_password)
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        // Show M_WEAK_PASSWORD error in the password field
        if (throwable is Failure.ServerError
                && throwable.error.code == MatrixError.M_WEAK_PASSWORD) {
            passwordFieldTil.error = errorFormatter.toHumanReadable(throwable)
        } else {
            loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
        }
    }

    override fun updateWithState(state: LoginViewState) {
        isSignupMode = state.signMode == SignMode.SignUp
        isNumericOnlyUserIdForbidden = state.serverType == ServerType.MatrixOrg

        setupUi(state)
        setupAutoFill(state)
        setupButtons(state)

        when (state.asyncLoginAction) {
            is Loading -> {
                // Ensure password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail -> {
                val error = state.asyncLoginAction.error
                if (error is Failure.ServerError
                        && error.error.code == MatrixError.M_FORBIDDEN
                        && error.error.message.isEmpty()) {
                    // Login with email, but email unknown
                    loginFieldTil.error = getString(R.string.login_login_with_email_error)
                } else {
                    // Trick to display the error without text.
                    loginFieldTil.error = " "
                    if (error.isInvalidPassword() && spaceInPassword()) {
                        passwordFieldTil.error = getString(R.string.auth_invalid_login_param_space_in_password)
                    } else {
                        passwordFieldTil.error = errorFormatter.toHumanReadable(error)
                    }
                }
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }

        when (state.asyncRegistration) {
            is Loading -> {
                // Ensure password is hidden
                passwordShown = false
                renderPasswordField()
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }

    /**
     * Detect if password ends or starts with spaces
     */
    private fun spaceInPassword() = passwordField.text.toString().let { it.trim() != it }
}
