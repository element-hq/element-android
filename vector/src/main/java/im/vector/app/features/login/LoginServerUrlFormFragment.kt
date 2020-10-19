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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import butterknife.OnClick
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.openUrlInChromeCustomTab
import kotlinx.android.synthetic.main.fragment_login_server_url_form.*
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * In this screen, the user is prompted to enter a homeserver url
 */
class LoginServerUrlFormFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_server_url_form

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeServerField()
    }

    private fun setupHomeServerField() {
        loginServerUrlFormHomeServerUrl.textChanges()
                .subscribe {
                    loginServerUrlFormHomeServerUrlTil.error = null
                    loginServerUrlFormSubmit.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

        loginServerUrlFormHomeServerUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupUi(state: LoginViewState) {
        when (state.serverType) {
            ServerType.EMS -> {
                loginServerUrlFormIcon.isVisible = true
                loginServerUrlFormTitle.text = getString(R.string.login_connect_to_modular)
                loginServerUrlFormText.text = getString(R.string.login_server_url_form_modular_text)
                loginServerUrlFormLearnMore.isVisible = true
                loginServerUrlFormHomeServerUrlTil.hint = getText(R.string.login_server_url_form_modular_hint)
                loginServerUrlFormNotice.text = getString(R.string.login_server_url_form_common_notice)
            }
            else           -> {
                loginServerUrlFormIcon.isVisible = false
                loginServerUrlFormTitle.text = getString(R.string.login_server_other_title)
                loginServerUrlFormText.text = getString(R.string.login_connect_to_a_custom_server)
                loginServerUrlFormLearnMore.isVisible = false
                loginServerUrlFormHomeServerUrlTil.hint = getText(R.string.login_server_url_form_other_hint)
                loginServerUrlFormNotice.text = getString(R.string.login_server_url_form_common_notice)
            }
        }
    }

    @OnClick(R.id.loginServerUrlFormLearnMore)
    fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerUrl)
    }

    @SuppressLint("SetTextI18n")
    @OnClick(R.id.loginServerUrlFormSubmit)
    fun submit() {
        cleanupUi()

        // Static check of homeserver url, empty, malformed, etc.
        val serverUrl = loginServerUrlFormHomeServerUrl.text.toString().trim().ensureProtocol()

        when {
            serverUrl.isBlank() -> {
                loginServerUrlFormHomeServerUrlTil.error = getString(R.string.login_error_invalid_home_server)
            }
            else                -> {
                loginServerUrlFormHomeServerUrl.setText(serverUrl)
                loginViewModel.handle(LoginAction.UpdateHomeServer(serverUrl))
            }
        }
    }

    private fun cleanupUi() {
        loginServerUrlFormSubmit.hideKeyboard()
        loginServerUrlFormHomeServerUrlTil.error = null
    }

    override fun onError(throwable: Throwable) {
        loginServerUrlFormHomeServerUrlTil.error = if (throwable is Failure.NetworkConnection
                && throwable.ioException is UnknownHostException) {
            // Invalid homeserver?
            getString(R.string.login_error_homeserver_not_found)
        } else {
            errorFormatter.toHumanReadable(throwable)
        }
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)

        if (state.loginMode != LoginMode.Unknown) {
            // The home server url is valid
            loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnLoginFlowRetrieved(state.loginMode == LoginMode.Sso)))
        }
    }
}
