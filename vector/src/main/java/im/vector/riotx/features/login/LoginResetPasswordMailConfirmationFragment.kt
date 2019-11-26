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
import android.view.View
import androidx.appcompat.app.AlertDialog
import butterknife.OnClick
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.error.is401
import kotlinx.android.synthetic.main.fragment_login_reset_password_mail_confirmation.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to check his email and to click on a button once it's done
 */
class LoginResetPasswordMailConfirmationFragment @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_reset_password_mail_confirmation

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
    }

    private fun setupUi() {
        resetPasswordMailConfirmationNotice.text = getString(R.string.login_reset_password_mail_confirmation_notice, loginViewModel.resetPasswordEmail)
    }

    @OnClick(R.id.resetPasswordMailConfirmationSubmit)
    fun submit() {
        loginViewModel.handle(LoginAction.ResetPasswordMailConfirmed)
    }

    override fun onError(throwable: Throwable) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun invalidate() = withState(loginViewModel) { state ->
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
                loginSharedActionViewModel.post(LoginNavigation.OnResetPasswordMailConfirmationSuccess)
            }
        }

        Unit
    }
}
