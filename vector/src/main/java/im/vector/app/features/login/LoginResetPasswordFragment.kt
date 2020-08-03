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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.isEmail
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.extensions.toReducedUrl
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_login_reset_password.*
import javax.inject.Inject

/**
 * In this screen, the user is asked for email and new password to reset his password
 */
class LoginResetPasswordFragment @Inject constructor() : AbstractLoginFragment() {

    private var passwordShown = false

    // Show warning only once
    private var showWarning = true

    override fun getLayoutResId() = R.layout.fragment_login_reset_password

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupPasswordReveal()
    }

    private fun setupUi(state: LoginViewState) {
        resetPasswordTitle.text = getString(R.string.login_reset_password_on, state.homeServerUrl.toReducedUrl())
    }

    private fun setupSubmitButton() {
        Observable
                .combineLatest(
                        resetPasswordEmail.textChanges().map { it.isEmail() },
                        passwordField.textChanges().map { it.isNotEmpty() },
                        BiFunction<Boolean, Boolean, Boolean> { isEmail, isPasswordNotEmpty ->
                            isEmail && isPasswordNotEmpty
                        }
                )
                .subscribeBy {
                    resetPasswordEmailTil.error = null
                    passwordFieldTil.error = null
                    resetPasswordSubmit.isEnabled = it
                }
                .disposeOnDestroyView()
    }

    @OnClick(R.id.resetPasswordSubmit)
    fun submit() {
        cleanupUi()

        if (showWarning) {
            showWarning = false
            // Display a warning as Riot-Web does first
            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.login_reset_password_warning_title)
                    .setMessage(R.string.login_reset_password_warning_content)
                    .setPositiveButton(R.string.login_reset_password_warning_submit) { _, _ ->
                        doSubmit()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            doSubmit()
        }
    }

    private fun doSubmit() {
        val email = resetPasswordEmail.text.toString()
        val password = passwordField.text.toString()

        loginViewModel.handle(LoginAction.ResetPassword(email, password))
    }

    private fun cleanupUi() {
        resetPasswordSubmit.hideKeyboard()
        resetPasswordEmailTil.error = null
        passwordFieldTil.error = null
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
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)

        when (state.asyncResetPassword) {
            is Loading -> {
                // Ensure new password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                resetPasswordEmailTil.error = errorFormatter.toHumanReadable(state.asyncResetPassword.error)
            }
            is Success -> {
                Unit
            }
        }
    }
}
