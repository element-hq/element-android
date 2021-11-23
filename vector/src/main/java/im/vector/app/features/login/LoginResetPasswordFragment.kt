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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import fr.gouv.tchap.features.platform.PlatformAction
import fr.gouv.tchap.features.platform.PlatformViewEvents
import fr.gouv.tchap.features.platform.PlatformViewModel
import fr.gouv.tchap.features.platform.PlatformViewState
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.isEmail
import im.vector.app.databinding.FragmentLoginResetPasswordBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

/**
 * In this screen, the user is asked for email and new password to reset his password
 */
class LoginResetPasswordFragment @Inject constructor(
        private val platformViewModelFactory: PlatformViewModel.Factory
) : AbstractLoginFragment<FragmentLoginResetPasswordBinding>(), PlatformViewModel.Factory {

    private val viewModel: PlatformViewModel by fragmentViewModel()

    // Show warning only once
    private var showWarning = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordBinding {
        return FragmentLoginResetPasswordBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()

        viewModel.observeViewEvents {
            when (it) {
                is PlatformViewEvents.Loading -> showLoading(it.message)
                is PlatformViewEvents.Failure -> {
                    // Dialog is displayed by the Activity
                }
                is PlatformViewEvents.Success -> updateHomeServer(it.platform)
            }.exhaustive
        }

        loginViewModel.observeViewEvents { loginViewEvents ->
            when (loginViewEvents) {
                LoginViewEvents.OnLoginFlowRetrieved ->
                    loginViewModel.handle(LoginAction.CheckPasswordPolicy(views.passwordField.text.toString()))
                LoginViewEvents.OnPasswordValidated  ->
                    submit()
                else                                 -> Unit // This is handled by the Activity
            }.exhaustive
        }
    }

    private fun setupSubmitButton() {
        views.resetPasswordSubmit.setOnClickListener {
            viewModel.handle(PlatformAction.DiscoverTchapPlatform(views.resetPasswordEmail.text.toString()))
        }

        combine(
                views.resetPasswordEmail.textChanges().map { it.isEmail() },
                views.passwordField.textChanges().map { it.isNotEmpty() }
        ) { isEmail, isPasswordNotEmpty ->
            isEmail && isPasswordNotEmpty
        }
                .onEach {
                    views.resetPasswordEmailTil.error = null
                    views.passwordFieldTil.error = null
                    views.resetPasswordSubmit.isEnabled = it
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun submit() {
        cleanupUi()

        if (showWarning) {
            showWarning = false
            val message = if (BuildConfig.IS_KEY_BACKUP_SUPPORTED) {
                R.string.login_reset_password_warning_content
            } else {
                R.string.tchap_login_reset_password_warning_content
            }
            // Display a warning as Riot-Web does first
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.login_reset_password_warning_title)
                    .setMessage(message)
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
        val email = views.resetPasswordEmail.text.toString()
        val password = views.passwordField.text.toString()

        loginViewModel.handle(LoginAction.ResetPassword(email, password))
    }

    private fun cleanupUi() {
        views.resetPasswordSubmit.hideKeyboard()
        views.resetPasswordEmailTil.error = null
        views.passwordFieldTil.error = null
    }

    private fun updateHomeServer(platform: Platform) {
        loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + platform.hs))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun updateWithState(state: LoginViewState) {
        when (state.asyncResetPassword) {
            is Loading -> {
                // Ensure new password is hidden
                views.passwordField.hidePassword()
            }
            is Fail    -> {
                views.resetPasswordEmailTil.error = errorFormatter.toHumanReadable(state.asyncResetPassword.error)
            }
            is Success -> Unit
        }
    }

    override fun create(initialState: PlatformViewState): PlatformViewModel {
        return platformViewModelFactory.create(initialState)
    }
}
