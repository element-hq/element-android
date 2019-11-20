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
import android.os.Parcelable
import android.text.InputType
import android.view.View
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.args
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.auth.registration.RegisterThreePid
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_generic_text_input_form.*
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

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
class LoginGenericTextInputFormFragment @Inject constructor(private val errorFormatter: ErrorFormatter) : AbstractLoginFragment() {

    private val params: LoginGenericTextInputFormFragmentArgument by args()

    override fun getLayoutResId() = R.layout.fragment_login_generic_text_input_form

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setupSubmitButton()
        setupTil()
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
                loginGenericTextInputFormTil.hint = getString(if (params.mandatory) R.string.login_set_email_mandatory_hint else R.string.login_set_email_optional_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                loginGenericTextInputFormOtherButton.isVisible = false
                loginGenericTextInputFormSubmit.text = getString(R.string.login_set_email_submit)
            }
            TextInputFormFragmentMode.SetMsisdn     -> {
                loginGenericTextInputFormTitle.text = getString(R.string.login_set_msisdn_title)
                loginGenericTextInputFormNotice.text = getString(R.string.login_set_msisdn_notice)
                loginGenericTextInputFormTil.hint = getString(if (params.mandatory) R.string.login_set_msisdn_mandatory_hint else R.string.login_set_msisdn_optional_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_PHONE
                loginGenericTextInputFormOtherButton.isVisible = false
                loginGenericTextInputFormSubmit.text = getString(R.string.login_set_msisdn_submit)
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                loginGenericTextInputFormTitle.text = getString(R.string.login_msisdn_confirm_title)
                loginGenericTextInputFormNotice.text = getString(R.string.login_msisdn_confirm_notice, params.extra)
                loginGenericTextInputFormTil.hint = getString(R.string.login_msisdn_confirm_hint)
                loginGenericTextInputFormTextInput.inputType = InputType.TYPE_CLASS_NUMBER
                loginGenericTextInputFormOtherButton.isVisible = true
                loginGenericTextInputFormOtherButton.text = getString(R.string.login_msisdn_confirm_send_again)
                loginGenericTextInputFormSubmit.text = getString(R.string.login_msisdn_confirm_submit)
            }
        }
    }

    @OnClick(R.id.loginGenericTextInputFormOtherButton)
    fun onOtherButtonClicked() {
        // TODO
    }

    @OnClick(R.id.loginGenericTextInputFormSubmit)
    fun onSubmitClicked() {
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
                    // TODO Country code
                    loginViewModel.handle(LoginAction.AddThreePid(RegisterThreePid.Msisdn(text, "FR")))
                }
                TextInputFormFragmentMode.ConfirmMsisdn -> {
                    loginViewModel.handle(LoginAction.ValidateThreePid(text))
                }
            }
        }
    }

    private fun setupSubmitButton() {
        if (params.mandatory) {
            loginGenericTextInputFormSubmit.isEnabled = false
            loginGenericTextInputFormTextInput.textChanges()
                    .subscribe {
                        // TODO Better check for email format, etc?
                        loginGenericTextInputFormSubmit.isEnabled = it.isNotBlank()
                    }
                    .disposeOnDestroyView()
        } else {
            loginGenericTextInputFormSubmit.isEnabled = true
        }
    }

    override fun onRegistrationError(throwable: Throwable) {
        when (params.mode) {
            TextInputFormFragmentMode.SetEmail      -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the mail waiting screen
                    loginSharedActionViewModel.post(LoginNavigation.OnSendEmailSuccess(loginViewModel.currentThreePid ?: ""))
                } else {
                    loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.SetMsisdn     -> {
                if (throwable.is401()) {
                    // This is normal use case, we go to the enter code screen
                    loginSharedActionViewModel.post(LoginNavigation.OnSendMsisdnSuccess(loginViewModel.currentThreePid ?: ""))
                } else {
                    loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
            TextInputFormFragmentMode.ConfirmMsisdn -> {
                if (throwable is Failure.SuccessError) {
                    // The entered code is not correct
                    loginGenericTextInputFormTil.error = getString(R.string.login_validation_code_is_not_correct)
                } else {
                    loginGenericTextInputFormTil.error = errorFormatter.toHumanReadable(throwable)
                }
            }
        }
    }

    private fun Throwable.is401(): Boolean {
        return (this is Failure.ServerError && this.httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED /* 401 */
                && this.error.code == MatrixError.UNAUTHORIZED)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
