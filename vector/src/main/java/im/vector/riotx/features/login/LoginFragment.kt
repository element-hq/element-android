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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.airbnb.mvrx.*
import com.jakewharton.rxbinding3.view.focusChanges
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.riotx.R
import im.vector.riotx.core.extensions.setTextWithColoredPart
import im.vector.riotx.core.extensions.showPassword
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import im.vector.riotx.features.homeserver.ServerUrlsRepository
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_login.*
import javax.inject.Inject

/**
 * What can be improved:
 * - When filtering more (when entering new chars), we could filter on result we already have, during the new server request, to avoid empty screen effect
 */
class LoginFragment @Inject constructor() : VectorBaseFragment() {

    private val viewModel: LoginViewModel by activityViewModel()

    private var passwordShown = false

    override fun getLayoutResId() = R.layout.fragment_login

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotice()
        setupAuthButton()
        setupPasswordReveal()

        homeServerField.focusChanges()
                .subscribe {
                    if (!it) {
                        viewModel.handle(LoginAction.UpdateHomeServer(homeServerField.text.toString()))
                    }
                }
                .disposeOnDestroy()

        homeServerField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.handle(LoginAction.UpdateHomeServer(homeServerField.text.toString()))
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        val initHsUrl = viewModel.getInitialHomeServerUrl()
        if (initHsUrl != null) {
            homeServerField.setText(initHsUrl)
        } else {
            homeServerField.setText(ServerUrlsRepository.getDefaultHomeServerUrl(requireContext()))
        }
        viewModel.handle(LoginAction.UpdateHomeServer(homeServerField.text.toString()))
    }

    private fun setupNotice() {
        riotx_no_registration_notice.setTextWithColoredPart(R.string.riotx_no_registration_notice, R.string.riotx_no_registration_notice_colored_part)

        riotx_no_registration_notice.setOnClickListener {
            openUrlInExternalBrowser(requireActivity(), "https://about.riot.im/downloads")
        }
    }

    private fun authenticate() {
        val login = loginField.text?.trim().toString()
        val password = passwordField.text?.trim().toString()

        viewModel.handle(LoginAction.Login(login, password))
    }

    private fun setupAuthButton() {
        Observable
                .combineLatest(
                        loginField.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.trim().isNotEmpty() },
                        homeServerField.textChanges().map { it.trim().isNotEmpty() },
                        Function3<Boolean, Boolean, Boolean, Boolean> { isLoginNotEmpty, isPasswordNotEmpty, isHomeServerNotEmpty ->
                            isLoginNotEmpty && isPasswordNotEmpty && isHomeServerNotEmpty
                        }
                )
                .subscribeBy { authenticateButton.isEnabled = it }
                .disposeOnDestroy()
        authenticateButton.setOnClickListener { authenticate() }

        authenticateButtonSso.setOnClickListener { openSso() }
    }

    private fun openSso() {
        viewModel.handle(LoginAction.NavigateTo(LoginActivity.Navigation.OpenSsoLoginFallback))
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

    override fun invalidate() = withState(viewModel) { state ->
        TransitionManager.beginDelayedTransition(login_fragment)

        when (state.asyncHomeServerLoginFlowRequest) {
            is Incomplete -> {
                progressBar.isVisible = true
                touchArea.isVisible = true
                loginField.isVisible = false
                passwordContainer.isVisible = false
                authenticateButton.isVisible = false
                authenticateButtonSso.isVisible = false
                passwordShown = false
                renderPasswordField()
            }
            is Fail       -> {
                progressBar.isVisible = false
                touchArea.isVisible = false
                loginField.isVisible = false
                passwordContainer.isVisible = false
                authenticateButton.isVisible = false
                authenticateButtonSso.isVisible = false
                Toast.makeText(requireActivity(), "Authenticate failure: ${state.asyncHomeServerLoginFlowRequest.error}", Toast.LENGTH_LONG).show()
            }
            is Success    -> {
                progressBar.isVisible = false
                touchArea.isVisible = false

                when (state.asyncHomeServerLoginFlowRequest()) {
                    LoginMode.Password    -> {
                        loginField.isVisible = true
                        passwordContainer.isVisible = true
                        authenticateButton.isVisible = true
                        authenticateButtonSso.isVisible = false
                        if (loginField.text.isNullOrBlank() && passwordField.text.isNullOrBlank()) {
                            // Jump focus to login
                            loginField.requestFocus()
                        }
                    }
                    LoginMode.Sso         -> {
                        loginField.isVisible = false
                        passwordContainer.isVisible = false
                        authenticateButton.isVisible = false
                        authenticateButtonSso.isVisible = true
                    }
                    LoginMode.Unsupported -> {
                        loginField.isVisible = false
                        passwordContainer.isVisible = false
                        authenticateButton.isVisible = false
                        authenticateButtonSso.isVisible = false
                        Toast.makeText(requireActivity(), "None of the homeserver login mode is supported by RiotX", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        when (state.asyncLoginAction) {
            is Loading -> {
                progressBar.isVisible = true
                touchArea.isVisible = true

                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                progressBar.isVisible = false
                touchArea.isVisible = false
                Toast.makeText(requireActivity(), "Authenticate failure: ${state.asyncLoginAction.error}", Toast.LENGTH_LONG).show()
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }
}
