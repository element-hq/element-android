/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.autofill.HintConstants
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.FragmentLoginGenericTextInputFormBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.extensions.isEmail
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.is401
import reactivecircus.flowbinding.android.widget.textChanges

enum class TextInputFormFragmentMode {
    SetEmail,
    SetMsisdn,
    ConfirmMsisdn
}

@Parcelize
data class LoginGenericTextInputFormFragmentArgument(
        val mode: TextInputFormFragmentMode,
        val mandatory: Boolean,
        val extra: String = ""
) : Parcelable

/**
 * In this screen, the user is asked for a text input.
 */
@AndroidEntryPoint
class LoginGenericTextInputFormFragment :
        AbstractLoginFragment<FragmentLoginGenericTextInputFormBinding>() {

    private val params: LoginGenericTextInputFormFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginGenericTextInputFormBinding {
        return FragmentLoginGenericTextInputFormBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupUi()
        setupSubmitButton()
        setupTil()
        setupAutoFill()
    }

    private fun setupViews() {
        views.loginGenericTextInputFormOtherButton.debouncedClicks { onOtherButtonClicked() }
        views.loginGenericTextInputFormSubmit.debouncedClicks { submit() }
    }

    private fun setupAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            views.loginGenericTextInputFormTextInput.setAutofillHints(
                    when (params.mode) {
                        TextInputFormFragmentMode.SetEmail -> HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS
                        TextInputFormFragmentMode.SetMsisdn -> HintConstants.AUTOFILL_HINT_PHONE_NUMBER
                        TextInputFormFragmentMode.ConfirmMsisdn -> HintConstants.AUTOFILL_HINT_SMS_OTP
                    }
            )
        }
    }

    private fun setupTil() {
        views.loginGenericTextInputFormTextInput.textChanges()
                .onEach {
                    views.loginGenericTextInputFormTil.error = null
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupUi() {
        when (params.mode) {
            TextInputFormFragmentMode.SetEmail -> {
                views.loginGenericTextInputFormTitle.text = getString(CommonStrings.login_set_email_title)
                views.loginGenericTextInputFormNotice.text = getString(CommonStrings.login_set_email_notice)
                views.loginGenericTextInputFormNotice2.setTextOrHide(null)
                views.loginGenericTextInputFormTil.hint =
                        getString(if (params.mandatory) CommonStrings.login_set_email_mandatory_hint else CommonStrings.login_set_email_optional_hint)
                views.loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                views.loginGenericTextInputFormOtherButton.isVisible = false
                views.loginGenericTextInputFormSubmit.text = getString(CommonStrings.login_set_email_submit)
            }
            TextInputFormFragmentMode.SetMsisdn -> {
                views.loginGenericTextInputFormTitle.text = getString(CommonStrings.login_set_msisdn_title)
                views.loginGenericTextInputFormNotice.text = getString(CommonStrings.login_set_msisdn_notice)
                views.loginGenericTextInputFormNotice2.setTextOrHide(getString(CommonStrings.login_set_msisdn_notice2))
                views.loginGenericTextInputFormTil.hint =
                        getString(if (params.mandatory) CommonStrings.login_set_msisdn_mandatory_hint else CommonStrings.login_set_msisdn_optional_hint)
                views.loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_PHONE
                views.loginGenericTextInputFormOtherButton.isVisible = false
                views.loginGenericTextInputFormSubmit.text = getString(CommonStrings.login_set_msisdn_submit)
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                views.loginGenericTextInputFormTitle.text = getString(CommonStrings.login_msisdn_confirm_title)
                views.loginGenericTextInputFormNotice.text = getString(CommonStrings.login_msisdn_confirm_notice, params.extra)
                views.loginGenericTextInputFormNotice2.setTextOrHide(null)
                views.loginGenericTextInputFormTil.hint =
                        getString(CommonStrings.login_msisdn_confirm_hint)
                views.loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_NUMBER
                views.loginGenericTextInputFormOtherButton.isVisible = true
                views.loginGenericTextInputFormOtherButton.text = getString(CommonStrings.login_msisdn_confirm_send_again)
                views.loginGenericTextInputFormSubmit.text = getString(CommonStrings.login_msisdn_confirm_submit)
            }
        }
    }

    private fun onOtherButtonClicked() {
        when (params.mode) {
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                loginViewModel.handle(LoginAction.SendAgainThreePid)
            }
            else -> {
                // Should not happen, button is not displayed
            }
        }
    }

    private fun submit() {
        cleanupUi()
        val text = views.loginGenericTextInputFormTextInput.text.toString()

        if (text.isEmpty()) {
            // Perform dummy action
            loginViewModel.handle(LoginAction.RegisterDummy)
        } else {
            when (params.mode) {
                TextInputFormFragmentMode.SetEmail -> {
                    loginViewModel.handle(LoginAction.AddThreePid(RegisterThreePid.Email(text)))
                }
                TextInputFormFragmentMode.SetMsisdn -> {
                    getCountryCodeOrShowError(text)?.let { countryCode ->
                        loginViewModel.handle(LoginAction.AddThreePid(RegisterThreePid.Msisdn(text, countryCode)))
                    }
                }
                TextInputFormFragmentMode.ConfirmMsisdn -> {
                    loginViewModel.handle(LoginAction.ValidateThreePid(text))
                }
            }
        }
    }

    private fun cleanupUi() {
        views.loginGenericTextInputFormSubmit.hideKeyboard()
        views.loginGenericTextInputFormSubmit.error = null
    }

    private fun getCountryCodeOrShowError(text: String): String? {
        // We expect an international format for the moment (see https://github.com/element-hq/riotX-android/issues/693)
        if (text.startsWith("+")) {
            try {
                val phoneNumber = PhoneNumberUtil.getInstance().parse(text, null)
                return PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(phoneNumber.countryCode)
            } catch (e: NumberParseException) {
                views.loginGenericTextInputFormTil.error = getString(CommonStrings.login_msisdn_error_other)
            }
        } else {
            views.loginGenericTextInputFormTil.error = getString(CommonStrings.login_msisdn_error_not_international)
        }

        // Error
        return null
    }

    private fun setupSubmitButton() {
        views.loginGenericTextInputFormSubmit.isEnabled = false
        views.loginGenericTextInputFormTextInput.textChanges()
                .onEach {
                    views.loginGenericTextInputFormSubmit.isEnabled = isInputValid(it)
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun isInputValid(input: CharSequence): Boolean {
        return if (input.isEmpty() && !params.mandatory) {
            true
        } else {
            when (params.mode) {
                TextInputFormFragmentMode.SetEmail -> {
                    input.isEmail()
                }
                TextInputFormFragmentMode.SetMsisdn -> {
                    input.isNotBlank()
                }
                TextInputFormFragmentMode.ConfirmMsisdn -> {
                    input.isNotBlank()
                }
            }
        }
    }

    override fun onError(throwable: Throwable) {
        when (params.mode) {
            TextInputFormFragmentMode.SetEmail -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the mail waiting screen
                    loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSendEmailSuccess(loginViewModel.currentThreePid ?: "")))
                } else {
                    views.loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.SetMsisdn -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the enter code screen
                    loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSendMsisdnSuccess(loginViewModel.currentThreePid ?: "")))
                } else {
                    views.loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                when {
                    throwable is Failure.SuccessError ->
                        // The entered code is not correct
                        views.loginGenericTextInputFormTil.error = getString(CommonStrings.login_validation_code_is_not_correct)
                    throwable.is401() ->
                        // It can happen if user request again the 3pid
                        Unit
                    else ->
                        views.loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
