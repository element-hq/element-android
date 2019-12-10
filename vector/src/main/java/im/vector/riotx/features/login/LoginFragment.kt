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

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.autofill.HintConstants
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.showPassword
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_login.*
import javax.inject.Inject

/**
 * In this screen, in signin mode:
 * - the user is asked for login and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class LoginFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    private var passwordShown = false

    override fun getLayoutResId() = R.layout.fragment_login

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupPasswordReveal()
    }

    private fun setupAutoFill(state: LoginViewState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp  -> {
                    loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
                    passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                }
                SignMode.SignIn  -> {
                    loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_USERNAME)
                    passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
                }
            }
        }
    }

    @OnClick(R.id.loginSubmit)
    fun submit() {
        cleanupUi()

        val login = loginField.text.toString()
        val password = passwordField.text.toString()

        loginViewModel.handle(LoginAction.LoginOrRegister(login, password, getString(R.string.login_mobile_device)))
    }

    private fun cleanupUi() {
        loginSubmit.hideKeyboard()
        loginFieldTil.error = null
        passwordFieldTil.error = null
    }

    private fun setupUi(state: LoginViewState) {
        val resId = when (state.signMode) {
            SignMode.Unknown -> error("developer error")
            SignMode.SignUp  -> R.string.login_signup_to
            SignMode.SignIn  -> R.string.login_connect_to
        }

        when (state.serverType) {
            ServerType.MatrixOrg -> {
                loginServerIcon.isVisible = true
                loginServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                loginTitle.text = getString(resId, state.homeServerUrlSimple)
                loginNotice.text = getString(R.string.login_server_matrix_org_text)
            }
            ServerType.Modular   -> {
                loginServerIcon.isVisible = true
                loginServerIcon.setImageResource(R.drawable.ic_logo_modular)
                loginTitle.text = getString(resId, "Modular")
                loginNotice.text = getString(R.string.login_server_modular_text)
            }
            ServerType.Other     -> {
                loginServerIcon.isVisible = false
                loginTitle.text = getString(resId, state.homeServerUrlSimple)
                loginNotice.text = getString(R.string.login_server_other_text)
            }
        }
    }

    private fun setupButtons(state: LoginViewState) {
        forgetPasswordButton.isVisible = state.signMode == SignMode.SignIn

        loginSubmit.text = getString(when (state.signMode) {
            SignMode.Unknown -> error("developer error")
            SignMode.SignUp  -> R.string.login_signup_submit
            SignMode.SignIn  -> R.string.login_signin
        })
    }

    private fun setupSubmitButton() {
        Observable
                .combineLatest(
                        loginField.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.trim().isNotEmpty() },
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
        loginSharedActionViewModel.post(LoginNavigation.OnForgetPasswordClicked)
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
            passwordReveal.setImageResource(R.drawable.ic_eye_closed_black)
            passwordReveal.contentDescription = getString(R.string.a11y_hide_password)
        } else {
            passwordReveal.setImageResource(R.drawable.ic_eye_black)
            passwordReveal.contentDescription = getString(R.string.a11y_show_password)
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)
        setupAutoFill(state)
        setupButtons(state)

        when (state.asyncLoginAction) {
            is Loading -> {
                // Ensure password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                val error = state.asyncLoginAction.error
                if (error is Failure.ServerError
                        && error.error.code == MatrixError.M_FORBIDDEN
                        && error.error.message.isEmpty()) {
                    // Login with email, but email unknown
                    loginFieldTil.error = getString(R.string.login_login_with_email_error)
                } else {
                    // Trick to display the error without text.
                    loginFieldTil.error = " "
                    passwordFieldTil.error = errorFormatter.toHumanReadable(state.asyncLoginAction.error)
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
}
