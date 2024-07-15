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
import androidx.autofill.HintConstants
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.hasSurroundingSpaces
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.isMatrixId
import im.vector.app.core.extensions.onTextChange
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.setOnFocusLostListener
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueCombinedRegisterBinding
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.SocialLoginButtonsView
import im.vector.app.features.login.render
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingAction.AuthenticateAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.matrix.android.sdk.api.auth.SSOAction
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isInvalidUsername
import org.matrix.android.sdk.api.failure.isLoginEmailUnknown
import org.matrix.android.sdk.api.failure.isRegistrationDisabled
import org.matrix.android.sdk.api.failure.isUsernameInUse
import org.matrix.android.sdk.api.failure.isWeakPassword
import reactivecircus.flowbinding.android.widget.textChanges

private const val MINIMUM_PASSWORD_LENGTH = 8

@AndroidEntryPoint
class FtueAuthCombinedRegisterFragment :
        AbstractSSOFtueAuthFragment<FragmentFtueCombinedRegisterBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueCombinedRegisterBinding {
        return FragmentFtueCombinedRegisterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSubmitButton()
        views.createAccountRoot.realignPercentagesToParent()
        views.editServerButton.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.EditServerSelection)) }
        views.createAccountPasswordInput.setOnImeDoneListener {
            if (canSubmit(views.createAccountInput.content(), views.createAccountPasswordInput.content())) {
                submit()
            }
        }

        views.createAccountInput.onTextChange(viewLifecycleOwner) {
            viewModel.handle(OnboardingAction.ResetSelectedRegistrationUserName)
            views.createAccountEntryFooter.text = null
        }

        views.createAccountInput.setOnFocusLostListener(viewLifecycleOwner) {
            viewModel.handle(OnboardingAction.UserNameEnteredAction.Registration(views.createAccountInput.content()))
        }
    }

    private fun canSubmit(account: CharSequence, password: CharSequence): Boolean {
        val accountIsValid = account.isNotEmpty()
        val passwordIsValid = password.length >= MINIMUM_PASSWORD_LENGTH
        return accountIsValid && passwordIsValid
    }

    private fun setupSubmitButton() {
        views.createAccountSubmit.setOnClickListener { submit() }
        views.createAccountInput.clearErrorOnChange(viewLifecycleOwner)
        views.createAccountPasswordInput.clearErrorOnChange(viewLifecycleOwner)

        combine(views.createAccountInput.editText().textChanges(), views.createAccountPasswordInput.editText().textChanges()) { account, password ->
            views.createAccountSubmit.isEnabled = canSubmit(account, password)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun submit() {
        withState(viewModel) { state ->
            cleanupUi()

            val login = views.createAccountInput.content()
            val password = views.createAccountPasswordInput.content()

            // This can be called by the IME action, so deal with empty cases
            var error = 0
            if (login.isEmpty()) {
                views.createAccountInput.error = getString(CommonStrings.error_empty_field_choose_user_name)
                error++
            }
            if (state.isNumericOnlyUserIdForbidden() && login.isDigitsOnly()) {
                views.createAccountInput.error = getString(CommonStrings.error_forbidden_digits_only_username)
                error++
            }
            if (password.isEmpty()) {
                views.createAccountPasswordInput.error = getString(CommonStrings.error_empty_field_choose_password)
                error++
            }

            if (error == 0) {
                val initialDeviceName = getString(CommonStrings.login_default_session_public_name)
                val registerAction = when {
                    login.isMatrixId() -> AuthenticateAction.RegisterWithMatrixId(login, password, initialDeviceName)
                    else -> AuthenticateAction.Register(login, password, initialDeviceName)
                }
                viewModel.handle(registerAction)
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
            throwable.isUsernameInUse() || throwable.isInvalidUsername() -> {
                views.createAccountInput.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isLoginEmailUnknown() -> {
                views.createAccountInput.error = getString(CommonStrings.login_login_with_email_error)
            }
            throwable.isInvalidPassword() && views.createAccountPasswordInput.hasSurroundingSpaces() -> {
                views.createAccountPasswordInput.error = getString(CommonStrings.auth_invalid_login_param_space_in_password)
            }
            throwable.isWeakPassword() || throwable.isInvalidPassword() -> {
                views.createAccountPasswordInput.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isHomeserverUnavailable() -> {
                views.createAccountInput.error = getString(CommonStrings.login_error_homeserver_not_found)
            }
            throwable.isRegistrationDisabled() -> {
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
        setupUi(state)
        setupAutoFill()
    }

    private fun setupUi(state: OnboardingViewState) {
        views.selectedServerName.text = state.selectedHomeserver.userFacingUrl.toReducedUrl()

        if (state.isLoading) {
            // Ensure password is hidden
            views.createAccountPasswordInput.editText().hidePassword()
        }

        views.createAccountEntryFooter.text = when {
            state.registrationState.isUserNameAvailable -> getString(
                    CommonStrings.ftue_auth_create_account_username_entry_footer,
                    state.registrationState.selectedMatrixId
            )

            else -> ""
        }

        when (state.selectedHomeserver.preferredLoginMode) {
            is LoginMode.SsoAndPassword -> renderSsoProviders(state.deviceId, state.selectedHomeserver.preferredLoginMode)
            else -> hideSsoProviders()
        }
    }

    private fun renderSsoProviders(deviceId: String?, loginMode: LoginMode) {
        views.ssoGroup.isVisible = true
        views.ssoButtons.render(loginMode, SocialLoginButtonsView.Mode.MODE_CONTINUE) { provider ->
            viewModel.fetchSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = deviceId,
                    provider = provider,
                    action = SSOAction.REGISTER
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

    private fun OnboardingViewState.isNumericOnlyUserIdForbidden() =
            selectedHomeserver.userFacingUrl == getString(im.vector.app.config.R.string.matrix_org_server_url)
}
