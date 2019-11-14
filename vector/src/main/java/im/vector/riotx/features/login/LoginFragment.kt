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
import androidx.transition.TransitionManager
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
import javax.inject.Inject

/**
 * In this screen, the user is asked for login and password to sign in to a homeserver.
 * He also can reset his password
 */
class LoginFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    // TODO Move to viewState?
    private var passwordShown = false

    override fun getLayoutResId() = R.layout.fragment_login

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLoginButton()
        setupPasswordReveal()
    }

    private fun authenticate() {
        val login = loginField.text?.trim().toString()
        val password = passwordField.text?.trim().toString()

        viewModel.handle(LoginAction.Login(login, password))
    }

    private fun setupLoginButton() {
        Observable
                .combineLatest(
                        loginField.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.trim().isNotEmpty() },
                        BiFunction<Boolean, Boolean, Boolean> { isLoginNotEmpty, isPasswordNotEmpty ->
                            isLoginNotEmpty && isPasswordNotEmpty
                        }
                )
                .subscribeBy { authenticateButton.isEnabled = it }
                .disposeOnDestroyView()
        authenticateButton.setOnClickListener { authenticate() }
    }

//    // TODO Move to server selection screen
//    private fun openSso() {
//        loginSharedActionViewModel.post(LoginNavigation.OpenSsoLoginFallback)
//    }

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
        viewModel.handle(LoginAction.ResetLogin)
    }

    override fun invalidate() = withState(viewModel) { state ->
        TransitionManager.beginDelayedTransition(login_fragment)

        when (state.asyncLoginAction) {
            is Loading -> {
                // Ensure password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                // TODO Handle error text properly
                // TODO Reset error when text is changed
                passwordFieldTil.error = errorFormatter.toHumanReadable(state.asyncLoginAction.error)
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }
}
