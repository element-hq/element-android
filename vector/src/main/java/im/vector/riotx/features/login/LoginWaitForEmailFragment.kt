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
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.args
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.error.is401
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_wait_for_email.*
import javax.inject.Inject

@Parcelize
data class LoginWaitForEmailFragmentArgument(
        val email: String
) : Parcelable

/**
 * In this screen, the user is asked to check his emails
 */
class LoginWaitForEmailFragment @Inject constructor(private val errorFormatter: ErrorFormatter) : AbstractLoginFragment() {

    private val params: LoginWaitForEmailFragmentArgument by args()

    override fun getLayoutResId() = R.layout.fragment_login_wait_for_email

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
    }

    override fun onResume() {
        super.onResume()

        loginViewModel.handle(LoginAction.CheckIfEmailHasBeenValidated(0))
    }

    override fun onPause() {
        super.onPause()

        loginViewModel.handle(LoginAction.StopEmailValidationCheck)
    }

    private fun setupUi() {
        loginWaitForEmailNotice.text = getString(R.string.login_wait_for_email_notice, params.email)
    }

    override fun onRegistrationError(throwable: Throwable) {
        if (throwable.is401()) {
            // Try again, with a delay
            loginViewModel.handle(LoginAction.CheckIfEmailHasBeenValidated(10_000))
        } else {
            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(errorFormatter.toHumanReadable(throwable))
                    .setPositiveButton(R.string.ok, null)
                    .show()
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
