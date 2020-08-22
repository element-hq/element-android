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

import androidx.appcompat.app.AlertDialog
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import im.vector.app.R
import org.matrix.android.sdk.api.failure.is401
import kotlinx.android.synthetic.main.fragment_login_reset_password_mail_confirmation.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to check his email and to click on a button once it's done
 */
class LoginResetPasswordMailConfirmationFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_reset_password_mail_confirmation

    private fun setupUi(state: LoginViewState) {
        resetPasswordMailConfirmationNotice.text = getString(R.string.login_reset_password_mail_confirmation_notice, state.resetPasswordEmail)
    }

    @OnClick(R.id.resetPasswordMailConfirmationSubmit)
    fun submit() {
        loginViewModel.handle(LoginAction.ResetPasswordMailConfirmed)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)

        when (state.asyncResetMailConfirmed) {
            is Fail    -> {
                // Link in email not yet clicked ?
                val message = if (state.asyncResetMailConfirmed.error.is401()) {
                    getString(R.string.auth_reset_password_error_unauthorized)
                } else {
                    errorFormatter.toHumanReadable(state.asyncResetMailConfirmed.error)
                }

                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
            is Success -> {
                Unit
            }
        }
    }
}
