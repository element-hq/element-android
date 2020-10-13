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

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.view.View
import androidx.autofill.HintConstants
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.args
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.isEmail
import im.vector.app.core.extensions.setTextOrHide
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_generic_text_input_form.*
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.is401
import javax.inject.Inject

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
 * In this screen, the user is asked for a text input
 */
class LoginGenericTextInputFormFragment @Inject constructor() : AbstractLoginFragment() {

    private val params: LoginGenericTextInputFormFragmentArgument by args()

    override fun getLayoutResId() = R.layout.fragment_login_generic_text_input_form

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setupSubmitButton()
        setupTil()
        setupAutoFill()
    }

    private fun setupAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loginGenericTextInputFormTextInput.setAutofillHints(
                    when (params.mode) {
                        TextInputFormFragmentMode.SetEmail      -> HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS
                        TextInputFormFragmentMode.SetMsisdn     -> HintConstants.AUTOFILL_HINT_PHONE_NUMBER
                        TextInputFormFragmentMode.ConfirmMsisdn -> HintConstants.AUTOFILL_HINT_SMS_OTP
                    }
            )
        }
    }

    private fun setupTil() {
        loginGenericTextInputFormTextInput.textChanges()
                .subscribe {
                    loginGenericTextInputFormTil.error = null
                }
                .disposeOnDestroyView()
    }

    private fun setupUi() {
        when (params.mode) {
            TextInputFormFragmentMode.SetEmail      -> {
                loginGenericTextInputFormTitle.text = getString(R.string.login_set_email_title)
                loginGenericTextInputFormNotice.text = getString(R.string.login_set_email_notice)
                loginGenericTextInputFormNotice2.setTextOrHide(null)
                loginGenericTextInputFormTil.hint =
                        getString(if (params.mandatory) R.string.login_set_email_mandatory_hint else R.string.login_set_email_optional_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                loginGenericTextInputFormOtherButton.isVisible = false
                loginGenericTextInputFormSubmit.text = getString(R.string.login_set_email_submit)
            }
            TextInputFormFragmentMode.SetMsisdn     -> {
                loginGenericTextInputFormTitle.text = getString(R.string.login_set_msisdn_title)
                loginGenericTextInputFormNotice.text = getString(R.string.login_set_msisdn_notice)
                loginGenericTextInputFormNotice2.setTextOrHide(getString(R.string.login_set_msisdn_notice2))
                loginGenericTextInputFormTil.hint =
                        getString(if (params.mandatory) R.string.login_set_msisdn_mandatory_hint else R.string.login_set_msisdn_optional_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_PHONE
                loginGenericTextInputFormOtherButton.isVisible = false
                loginGenericTextInputFormSubmit.text = getString(R.string.login_set_msisdn_submit)
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                loginGenericTextInputFormTitle.text = getString(R.string.login_msisdn_confirm_title)
                loginGenericTextInputFormNotice.text = getString(R.string.login_msisdn_confirm_notice, params.extra)
                loginGenericTextInputFormNotice2.setTextOrHide(null)
                loginGenericTextInputFormTil.hint =
                        getString(R.string.login_msisdn_confirm_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_NUMBER
                loginGenericTextInputFormOtherButton.isVisible = true
                loginGenericTextInputFormOtherButton.text = getString(R.string.login_msisdn_confirm_send_again)
                loginGenericTextInputFormSubmit.text = getString(R.string.login_msisdn_confirm_submit)
            }
        }
    }

    @OnClick(R.id.loginGenericTextInputFormOtherButton)
    fun onOtherButtonClicked() {
        when (params.mode) {
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                loginViewModel.handle(LoginAction.SendAgainThreePid)
            }
            else                                    -> {
                // Should not happen, button is not displayed
            }
        }
    }

    @OnClick(R.id.loginGenericTextInputFormSubmit)
    fun submit() {
        cleanupUi()
        val text = loginGenericTextInputFormTextInput.text.toString()

        if (text.isEmpty()) {
            // Perform dummy action
            loginViewModel.handle(LoginAction.RegisterDummy)
        } else {
            when (params.mode) {
                TextInputFormFragmentMode.SetEmail      -> {
                    loginViewModel.handle(LoginAction.AddThreePid(RegisterThreePid.Email(text)))
                }
                TextInputFormFragmentMode.SetMsisdn     -> {
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
        loginGenericTextInputFormSubmit.hideKeyboard()
        loginGenericTextInputFormSubmit.error = null
    }

    private fun getCountryCodeOrShowError(text: String): String? {
        // We expect an international format for the moment (see https://github.com/vector-im/riotX-android/issues/693)
        if (text.startsWith("+")) {
            try {
                val phoneNumber = PhoneNumberUtil.getInstance().parse(text, null)
                return PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(phoneNumber.countryCode)
            } catch (e: NumberParseException) {
                loginGenericTextInputFormTil.error = getString(R.string.login_msisdn_error_other)
            }
        } else {
            loginGenericTextInputFormTil.error = getString(R.string.login_msisdn_error_not_international)
        }

        // Error
        return null
    }

    private fun setupSubmitButton() {
        loginGenericTextInputFormSubmit.isEnabled = false
        loginGenericTextInputFormTextInput.textChanges()
                .subscribe {
                    loginGenericTextInputFormSubmit.isEnabled = isInputValid(it)
                }
                .disposeOnDestroyView()
    }

    private fun isInputValid(input: CharSequence): Boolean {
        return if (input.isEmpty() && !params.mandatory) {
            true
        } else {
            when (params.mode) {
                TextInputFormFragmentMode.SetEmail      -> {
                    input.isEmail()
                }
                TextInputFormFragmentMode.SetMsisdn     -> {
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
            TextInputFormFragmentMode.SetEmail      -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the mail waiting screen
                    loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSendEmailSuccess(loginViewModel.currentThreePid ?: "")))
                } else {
                    loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.SetMsisdn     -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the enter code screen
                    loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSendMsisdnSuccess(loginViewModel.currentThreePid ?: "")))
                } else {
                    loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                when {
                    throwable is Failure.SuccessError ->
                        // The entered code is not correct
                        loginGenericTextInputFormTil.error = getString(R.string.login_validation_code_is_not_correct)
                    throwable.is401()                 ->
                        // It can happen if user request again the 3pid
                        Unit
                    else                              ->
                        loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
