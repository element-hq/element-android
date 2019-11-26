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
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.riotx.R
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.net.ssl.HttpsURLConnection

/**
 * Parent Fragment for all the login/registration screens
 */
abstract class AbstractLoginFragment : VectorBaseFragment(), OnBackPressed {

    protected val loginViewModel: LoginViewModel by activityViewModel()
    protected lateinit var loginSharedActionViewModel: LoginSharedActionViewModel

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginSharedActionViewModel = activityViewModelProvider.get(LoginSharedActionViewModel::class.java)

        loginViewModel.viewEvents
                .observe()
                .subscribe {
                    handleLoginViewEvents(it)
                }
                .disposeOnDestroyView()
    }

    private fun handleLoginViewEvents(loginViewEvents: LoginViewEvents) {
        when (loginViewEvents) {
            is LoginViewEvents.Error -> showError(loginViewEvents.throwable)
            else                                                    ->
                // This is handled by the Activity
                Unit
        }
    }

    private fun showError(throwable: Throwable) {
        when (throwable) {
            is Failure.ServerError -> {
                if (throwable.error.code == MatrixError.FORBIDDEN
                        && throwable.httpCode == HttpsURLConnection.HTTP_FORBIDDEN /* 403 */) {
                    AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(getString(R.string.login_registration_disabled))
                            .setPositiveButton(R.string.ok, null)
                            .show()
                } else {
                    onError(throwable)
                }
            }
            else                   -> onError(throwable)
        }
    }

    abstract fun onError(throwable: Throwable)

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return when {
            loginViewModel.isRegistrationStarted  -> {
                // Ask for confirmation before cancelling the registration
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.login_signup_cancel_confirmation_title)
                        .setMessage(R.string.login_signup_cancel_confirmation_content)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            resetViewModel()
                            vectorBaseActivity.onBackPressed()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()

                true
            }
            loginViewModel.isResetPasswordStarted -> {
                // Ask for confirmation before cancelling the reset password
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.login_reset_password_cancel_confirmation_title)
                        .setMessage(R.string.login_reset_password_cancel_confirmation_content)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            resetViewModel()
                            vectorBaseActivity.onBackPressed()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()

                true
            }
            else                                  -> {
                resetViewModel()
                // Do not consume the Back event
                false
            }
        }
    }

    // Reset any modification on the loginViewModel by the current fragment
    abstract fun resetViewModel()
}
