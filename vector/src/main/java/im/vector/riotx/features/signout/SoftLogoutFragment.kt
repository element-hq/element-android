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

package im.vector.riotx.features.signout

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.autofill.HintConstants
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.*
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.riotx.R
import im.vector.riotx.core.dialogs.withColoredButton
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.extensions.showPassword
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.MainActivityArgs
import im.vector.riotx.features.login.LoginMode
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_soft_logout.*
import kotlinx.android.synthetic.main.item_error_retry.*
import javax.inject.Inject

/**
 * In this screen:
 * - the user is asked to enter a password to sign in again to a homeserver.
 * - or to cleanup all the data
 * TODO: migrate to Epoxy (along with all the login screen?)
 */
class SoftLogoutFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : VectorBaseFragment() {

    private var passwordShown = false

    private val softLogoutViewModel: SoftLogoutViewModel by activityViewModel()

    override fun getLayoutResId() = R.layout.fragment_soft_logout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupPasswordReveal()
        setupAutoFill()
    }

    private fun setupAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            softLogoutPasswordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
        }
    }

    @OnClick(R.id.itemErrorRetryButton)
    fun retry() {
        softLogoutViewModel.handle(SoftLogoutAction.RetryLoginFlow)
    }

    @OnClick(R.id.softLogoutSubmit)
    fun submit() {
        cleanupUi()

        val password = softLogoutPasswordField.text.toString()
        softLogoutViewModel.handle(SoftLogoutAction.SignInAgain(password))
    }

    @OnClick(R.id.softLogoutFormSsoSubmit)
    fun ssoSubmit() {
        // TODO
    }

    @OnClick(R.id.softLogoutClearDataSubmit)
    fun clearData() {
        withState(softLogoutViewModel) { state ->
            cleanupUi()

            val messageResId = if (state.hasUnsavedKeys) {
                R.string.soft_logout_clear_data_dialog_content
            } else {
                R.string.soft_logout_clear_data_dialog_e2e_warning_content
            }

            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.soft_logout_clear_data_dialog_title)
                    .setMessage(messageResId)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.soft_logout_clear_data_submit) { _, _ ->
                        MainActivity.restartApp(requireActivity(), MainActivityArgs(
                                clearCache = true,
                                clearCredentials = true,
                                isUserLoggedOut = true
                        ))
                    }
                    .show()
                    .withColoredButton(DialogInterface.BUTTON_POSITIVE)
        }
    }

    private fun cleanupUi() {
        softLogoutSubmit.hideKeyboard()
        softLogoutPasswordFieldTil.error = null
    }

    private fun setupUi(state: SoftLogoutViewState) {
        softLogoutNotice.text = getString(R.string.soft_logout_signin_notice,
                state.homeServerUrl,
                state.userDisplayName,
                state.userId)

        softLogoutE2eWarningNotice.isVisible = state.hasUnsavedKeys
    }

    private fun setupForm(state: SoftLogoutViewState) {
        softLogoutFormLoading.isVisible = state.asyncHomeServerLoginFlowRequest is Loading
        softLogoutFormSsoSubmit.isVisible = state.asyncHomeServerLoginFlowRequest.invoke() == LoginMode.Sso
        softLogoutFormPassword.isVisible = state.asyncHomeServerLoginFlowRequest.invoke() == LoginMode.Password
        softLogoutFormError.isVisible = state.asyncHomeServerLoginFlowRequest is Fail
        itemErrorRetryText.setTextOrHide((state.asyncHomeServerLoginFlowRequest as? Fail)?.error?.let { errorFormatter.toHumanReadable(it) })
    }

    private fun setupSubmitButton() {
        softLogoutPasswordField.textChanges()
                .map { it.trim().isNotEmpty() }
                .subscribeBy {
                    softLogoutPasswordFieldTil.error = null
                    softLogoutSubmit.isEnabled = it
                }
                .disposeOnDestroyView()
    }

    @OnClick(R.id.softLogoutForgetPasswordButton)
    fun forgetPasswordClicked() {
        // TODO
        // loginSharedActionViewModel.post(LoginNavigation.OnForgetPasswordClicked)
    }

    private fun setupPasswordReveal() {
        passwordShown = false

        softLogoutPasswordReveal.setOnClickListener {
            passwordShown = !passwordShown

            renderPasswordField()
        }

        renderPasswordField()
    }

    private fun renderPasswordField() {
        softLogoutPasswordField.showPassword(passwordShown)

        if (passwordShown) {
            softLogoutPasswordReveal.setImageResource(R.drawable.ic_eye_closed_black)
            softLogoutPasswordReveal.contentDescription = getString(R.string.a11y_hide_password)
        } else {
            softLogoutPasswordReveal.setImageResource(R.drawable.ic_eye_black)
            softLogoutPasswordReveal.contentDescription = getString(R.string.a11y_show_password)
        }
    }

    override fun invalidate() = withState(softLogoutViewModel) { state ->
        setupUi(state)
        setupForm(state)
        setupAutoFill()

        when (state.asyncLoginAction) {
            is Loading -> {
                // Ensure password is hidden
                passwordShown = false
                renderPasswordField()
            }
            is Fail    -> {
                softLogoutPasswordFieldTil.error = errorFormatter.toHumanReadable(state.asyncLoginAction.error)
            }
            // Success is handled by the SoftLogoutActivity
            is Success -> Unit
        }
    }
}
