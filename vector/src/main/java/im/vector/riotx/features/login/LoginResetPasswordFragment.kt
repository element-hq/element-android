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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.showPassword
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.passwordField
import kotlinx.android.synthetic.main.fragment_login.passwordFieldTil
import kotlinx.android.synthetic.main.fragment_login.passwordReveal
import kotlinx.android.synthetic.main.fragment_login_reset_password.*
import javax.inject.Inject

/**
 * In this screen, the user is asked for email and new password to reset his password
 */
class LoginResetPasswordFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    private var passwordShown = false

    override fun getLayoutResId() = R.layout.fragment_login_reset_password

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setupSubmitButton()
        setupPasswordReveal()
    }

    private fun setupUi() {
        resetPasswordTitle.text = getString(R.string.login_reset_password_on, loginViewModel.getHomeServerUrlSimple())
    }

    private fun setupSubmitButton() {
        Observable
                .combineLatest(
                        resetPasswordEmail.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.trim().isNotEmpty() },
                        BiFunction<Boolean, Boolean, Boolean> { isEmailNotEmpty, isPasswordNotEmpty ->
                            isEmailNotEmpty && isPasswordNotEmpty
                        }
                )
                .subscribeBy {
                    resetPasswordEmail.error = null
                    passwordFieldTil.error = null
                    resetPasswordSubmit.isEnabled = it
                }
                .disposeOnDestroyView()

        resetPasswordSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        val email = resetPasswordEmail.text?.trim().toString()
        val password = passwordField.text?.trim().toString()

        // TODO Add static check?

        loginViewModel.handle(LoginAction.ResetPassword(email, password))
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
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun onRegistrationError(throwable: Throwable) {
        // TODO
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun invalidate() = withState(loginViewModel) { state ->
        when (state.asyncResetPassword) {
            is Loading -> {
                // Ensure new password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                // TODO This does not work, we want the error to be on without text. Fix that
                resetPasswordEmailTil.error = ""
                // TODO Handle error text properly
                passwordFieldTil.error = errorFormatter.toHumanReadable(state.asyncResetPassword.error)
            }
            is Success -> {
                loginSharedActionViewModel.post(LoginNavigation.OnResetPasswordSuccess)
            }
        }
    }
}
