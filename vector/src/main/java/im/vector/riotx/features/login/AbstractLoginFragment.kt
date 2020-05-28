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

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.transition.TransitionInflater
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.riotx.R
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.net.ssl.HttpsURLConnection

/**
 * Parent Fragment for all the login/registration screens
 */
abstract class AbstractLoginFragment : VectorBaseFragment(), OnBackPressed {

    protected val loginViewModel: LoginViewModel by activityViewModel()

    private var isResetPasswordStarted = false

    // Due to async, we keep a boolean to avoid displaying twice the cancellation dialog
    private var displayCancelDialog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel.observeViewEvents {
            handleLoginViewEvents(it)
        }
    }

    private fun handleLoginViewEvents(loginViewEvents: LoginViewEvents) {
        when (loginViewEvents) {
            is LoginViewEvents.Failure -> showFailure(loginViewEvents.throwable)
            else                       ->
                // This is handled by the Activity
                Unit
        }.exhaustive
    }

    override fun showFailure(throwable: Throwable) {
        when (throwable) {
            is Failure.ServerError -> {
                if (throwable.error.code == MatrixError.M_FORBIDDEN
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

    open fun onError(throwable: Throwable) {
        super.showFailure(throwable)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return when {
            displayCancelDialog && loginViewModel.isRegistrationStarted -> {
                // Ask for confirmation before cancelling the registration
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.login_signup_cancel_confirmation_title)
                        .setMessage(R.string.login_signup_cancel_confirmation_content)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            displayCancelDialog = false
                            vectorBaseActivity.onBackPressed()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()

                true
            }
            displayCancelDialog && isResetPasswordStarted               -> {
                // Ask for confirmation before cancelling the reset password
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.login_reset_password_cancel_confirmation_title)
                        .setMessage(R.string.login_reset_password_cancel_confirmation_content)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            displayCancelDialog = false
                            vectorBaseActivity.onBackPressed()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()

                true
            }
            else                                                        -> {
                resetViewModel()
                // Do not consume the Back event
                false
            }
        }
    }

    final override fun invalidate() = withState(loginViewModel) { state ->
        // True when email is sent with success to the homeserver
        isResetPasswordStarted = state.resetPasswordEmail.isNullOrBlank().not()

        updateWithState(state)
    }

    open fun updateWithState(state: LoginViewState) {
        // No op by default
    }

    // Reset any modification on the loginViewModel by the current fragment
    abstract fun resetViewModel()
}
