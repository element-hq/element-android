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

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.autofill.HintConstants
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.setOnFocusLostListener
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueCombinedLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.SocialLoginButtonsView
import im.vector.app.features.login.SsoState
import im.vector.app.features.login.qr.QrCodeLoginArgs
import im.vector.app.features.login.qr.QrCodeLoginType
import im.vector.app.features.login.render
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import io.realm.Realm
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthCombinedLoginFragment :
        AbstractSSOFtueAuthFragment<FragmentFtueCombinedLoginBinding>() {

    @Inject lateinit var loginFieldsValidation: LoginFieldsValidation
    @Inject lateinit var loginErrorParser: LoginErrorParser
    @Inject lateinit var vectorFeatures: VectorFeatures
    @Inject lateinit var phoneNumberParser: PhoneNumberParser

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueCombinedLoginBinding {
        return FragmentFtueCombinedLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSubmitButton()
        views.loginRoot.realignPercentagesToParent()
        views.editServerButton.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.EditServerSelection)) }
        views.loginPasswordInput.setOnImeDoneListener { submit() }
        views.loginInput.setOnFocusLostListener(viewLifecycleOwner) {
            viewModel.handle(OnboardingAction.UserNameEnteredAction.Login(views.loginInput.content()))
        }
        views.loginForgotPassword.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnForgetPasswordClicked)) }

        viewModel.onEach(OnboardingViewState::canLoginWithQrCode) {
            configureQrCodeLoginButtonVisibility(it)
        }
    }

    private fun configureQrCodeLoginButtonVisibility(canLoginWithQrCode: Boolean) {
        views.loginWithQrCode.isVisible = canLoginWithQrCode
        if (canLoginWithQrCode) {
            views.loginWithQrCode.debouncedClicks {
                navigator
                        .openLoginWithQrCode(
                                requireActivity(),
                                QrCodeLoginArgs(
                                        loginType = QrCodeLoginType.LOGIN,
                                        showQrCodeImmediately = false,
                                )
                        )
            }
        }
    }

    private fun setupSubmitButton() {
        views.loginSubmit.setOnClickListener { submit() }
        views.loginInput.clearErrorOnChange(viewLifecycleOwner)
        views.loginPasswordInput.clearErrorOnChange(viewLifecycleOwner)

        combine(views.loginInput.editText().textChanges(), views.loginPasswordInput.editText().textChanges()) { account, password ->
            views.loginSubmit.isEnabled = account.isNotEmpty() && password.isNotEmpty()
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun submit() {
        cleanupUi()
        val number = views.loginInput.content()
        when (val result = phoneNumberParser.parseInternationalNumber(number)) {
            PhoneNumberParser.Result.ErrorInvalidNumber -> views.loginInput.error = getString(R.string.login_msisdn_error_other)
            PhoneNumberParser.Result.ErrorMissingInternationalCode -> views.loginInput.error = getString(R.string.login_msisdn_error_not_international)
            is PhoneNumberParser.Result.Success -> {
                val (countryCode, phoneNumber) = result
                loginFieldsValidation.validate(views.loginInput.content(), views.loginPasswordInput.content())
                        .onUsernameOrIdError { views.loginInput.error = it }
                        .onPasswordError { views.loginPasswordInput.error = it }
                        .onValid { _, _ ->
                            val initialDeviceName = getString(R.string.login_default_session_public_name)
                            val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
                            val editor = preferences?.edit()
                            editor?.putString("phone_number", phoneNumber)
                            editor?.apply()
                            viewModel.handle(
                                    OnboardingAction.AuthenticateAction.LoginPhoneNumber(
                                            countryCode,
                                            phoneNumber,
                                            phoneNumber,
                                            "12345678",
                                            initialDeviceName
                                    )
                            )
                        }
            }
        }
    }

    private fun cleanupUi() {
        views.loginSubmit.hideKeyboard()
        views.loginInput.error = null
        views.loginPasswordInput.error = null
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun onError(throwable: Throwable) {
        // Trick to display the error without text.
        views.loginInput.error = " "
        loginErrorParser.parse(throwable, views.loginPasswordInput.content())
                .onUnknown { super.onError(it) }
                .onUsernameOrIdError { views.loginInput.error = it }
                .onPasswordError { views.loginPasswordInput.error = it }
    }

    override fun updateWithState(state: OnboardingViewState) {
        setupUi(state)
        setupAutoFill()

        views.selectedServerName.text = state.selectedHomeserver.userFacingUrl.toReducedUrl()

        if (state.isLoading) {
            // Ensure password is hidden
            views.loginPasswordInput.editText().hidePassword()
        }
    }

    private fun setupUi(state: OnboardingViewState) {
        when (state.selectedHomeserver.preferredLoginMode) {
            is LoginMode.SsoAndPassword -> {
                showUsernamePassword()
                renderSsoProviders(state.deviceId, state.selectedHomeserver.preferredLoginMode.ssoState)
            }
            is LoginMode.Sso -> {
                hideUsernamePassword()
                renderSsoProviders(state.deviceId, state.selectedHomeserver.preferredLoginMode.ssoState)
            }
            else -> {
                showUsernamePassword()
                hideSsoProviders()
            }
        }
    }

    private fun renderSsoProviders(deviceId: String?, ssoState: SsoState) {
        views.ssoGroup.isVisible = true
        views.ssoButtonsHeader.isVisible = isUsernameAndPasswordVisible()
        views.ssoButtons.render(ssoState, SocialLoginButtonsView.Mode.MODE_CONTINUE) { id ->
            viewModel.fetchSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = deviceId,
                    provider = id
            )?.let { openInCustomTab(it) }
        }
    }

    private fun hideSsoProviders() {
        views.ssoGroup.isVisible = false
        views.ssoButtons.ssoIdentityProviders = null
    }

    private fun hideUsernamePassword() {
        views.loginEntryGroup.isVisible = false
    }

    private fun showUsernamePassword() {
        views.loginEntryGroup.isVisible = true
    }

    private fun isUsernameAndPasswordVisible() = views.loginEntryGroup.isVisible

    private fun setupAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            views.loginInput.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
            views.loginPasswordInput.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
        }
    }
}
