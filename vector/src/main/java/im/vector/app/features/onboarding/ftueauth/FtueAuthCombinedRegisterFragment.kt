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
import im.vector.app.R
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.hasContentFlow
import im.vector.app.core.extensions.hasSurroundingSpaces
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueCombinedRegisterBinding
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.SocialLoginButtonsView
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isInvalidUsername
import org.matrix.android.sdk.api.failure.isLoginEmailUnknown
import org.matrix.android.sdk.api.failure.isRegistrationDisabled
import org.matrix.android.sdk.api.failure.isUsernameInUse
import org.matrix.android.sdk.api.failure.isWeakPassword
import javax.inject.Inject

class FtueAuthCombinedRegisterFragment @Inject constructor() : AbstractSSOFtueAuthFragment<FragmentFtueCombinedRegisterBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueCombinedRegisterBinding {
        return FragmentFtueCombinedRegisterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSubmitButton()
        views.createAccountRoot.realignPercentagesToParent()
        views.editServerButton.debouncedClicks {
            viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.EditServerSelection))
        }

        views.createAccountPasswordInput.editText().setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupSubmitButton() {
        views.createAccountSubmit.setOnClickListener { submit() }
        observeInputFields()
                .onEach {
                    views.createAccountPasswordInput.error = null
                    views.createAccountInput.error = null
                    views.createAccountSubmit.isEnabled = it
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun observeInputFields() = combine(
            views.createAccountInput.hasContentFlow { it.trim() },
            views.createAccountPasswordInput.hasContentFlow(),
            transform = { isLoginNotEmpty, isPasswordNotEmpty -> isLoginNotEmpty && isPasswordNotEmpty }
    )

    private fun submit() {
        withState(viewModel) { state ->
            cleanupUi()

            val login = views.createAccountInput.content()
            val password = views.createAccountPasswordInput.content()

            // This can be called by the IME action, so deal with empty cases
            var error = 0
            if (login.isEmpty()) {
                views.createAccountInput.error = getString(R.string.error_empty_field_choose_user_name)
                error++
            }
            if (state.isNumericOnlyUserIdForbidden() && login.isDigitsOnly()) {
                views.createAccountInput.error = getString(R.string.error_forbidden_digits_only_username)
                error++
            }
            if (password.isEmpty()) {
                views.createAccountPasswordInput.error = getString(R.string.error_empty_field_choose_password)
                error++
            }

            if (error == 0) {
                viewModel.handle(OnboardingAction.Register(login, password, getString(R.string.login_default_session_public_name)))
            }
        }
    }

    private fun cleanupUi() {
        views.createAccountSubmit.hideKeyboard()
        views.createAccountInput.error = null
        views.createAccountPasswordInput.error = null
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun onError(throwable: Throwable) {
        // Trick to display the error without text.
        views.createAccountInput.error = " "
        when {
            throwable.isUsernameInUse() || throwable.isInvalidUsername()                             -> {
                views.createAccountInput.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isLoginEmailUnknown()                                                          -> {
                views.createAccountInput.error = getString(R.string.login_login_with_email_error)
            }
            throwable.isInvalidPassword() && views.createAccountPasswordInput.hasSurroundingSpaces() -> {
                views.createAccountPasswordInput.error = getString(R.string.auth_invalid_login_param_space_in_password)
            }
            throwable.isWeakPassword() || throwable.isInvalidPassword()                              -> {
                views.createAccountPasswordInput.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isRegistrationDisabled()                                                       -> {
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(getString(R.string.login_registration_disabled))
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
            else                                                                                     -> {
                super.onError(throwable)
            }
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        setupUi(state)
        setupAutoFill()

        views.selectedServerName.text = state.selectedHomeserver.userFacingUrl.toReducedUrl()
        views.selectedServerDescription.text = state.selectedHomeserver.description

        if (state.isLoading) {
            // Ensure password is hidden
            views.createAccountPasswordInput.editText().hidePassword()
        }
    }

    private fun setupUi(state: OnboardingViewState) {
        when (state.selectedHomeserver.preferredLoginMode) {
            is LoginMode.SsoAndPassword -> renderSsoProviders(state.deviceId, state.selectedHomeserver.preferredLoginMode.ssoIdentityProviders)
            else                        -> hideSsoProviders()
        }
    }

    private fun renderSsoProviders(deviceId: String?, ssoProviders: List<SsoIdentityProvider>?) {
        views.ssoGroup.isVisible = ssoProviders?.isNotEmpty() == true
        views.ssoButtons.mode = SocialLoginButtonsView.Mode.MODE_CONTINUE
        views.ssoButtons.ssoIdentityProviders = ssoProviders?.sorted()
        views.ssoButtons.listener = SocialLoginButtonsView.InteractionListener { id ->
            viewModel.getSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = deviceId,
                    providerId = id
            )?.let { openInCustomTab(it) }
        }
    }

    private fun hideSsoProviders() {
        views.ssoGroup.isVisible = false
        views.ssoButtons.ssoIdentityProviders = null
    }

    private fun setupAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            views.createAccountInput.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
            views.createAccountPasswordInput.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
        }
    }

    private fun OnboardingViewState.isNumericOnlyUserIdForbidden() = selectedHomeserver.userFacingUrl == getString(R.string.matrix_org_server_url)
}
