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

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.transition.TransitionInflater
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewModel
import im.vector.app.features.onboarding.OnboardingViewState
import kotlinx.coroutines.CancellationException

/**
 * Parent Fragment for all the login/registration screens.
 */
abstract class AbstractFtueAuthFragment<VB : ViewBinding> : VectorBaseFragment<VB>(), OnBackPressed {

    protected val viewModel: OnboardingViewModel by activityViewModel()

    private var isResetPasswordStarted = false

    // Due to async, we keep a boolean to avoid displaying twice the cancellation dialog
    private var displayCancelDialog = true
    protected open fun backIsHardExit() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let {
            sharedElementEnterTransition = TransitionInflater.from(it).inflateTransition(android.R.transition.move)
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            handleOnboardingViewEvents(it)
        }
    }

    private fun handleOnboardingViewEvents(viewEvents: OnboardingViewEvents) {
        when (viewEvents) {
            is OnboardingViewEvents.Failure -> showFailure(viewEvents.throwable)
            is OnboardingViewEvents.UnrecognisedCertificateFailure -> showUnrecognizedCertificateFailure(viewEvents)
            else ->
                // This is handled by the Activity
                Unit
        }
    }

    override fun showFailure(throwable: Throwable) {
        // Only the resumed Fragment can eventually show the error, to avoid multiple dialog display
        if (!isResumed) {
            return
        }

        when (throwable) {
            is CancellationException ->
                /* Ignore this error, user has cancelled the action */
                Unit
            else -> onError(throwable)
        }
    }

    private fun showUnrecognizedCertificateFailure(event: OnboardingViewEvents.UnrecognisedCertificateFailure) {
        // Ask the user to accept the certificate
        val cause = event.cause
        unrecognizedCertificateDialog.show(requireActivity(),
                cause.fingerprint,
                cause.url,
                object : UnrecognizedCertificateDialog.Callback {
                    override fun onAccept() {
                        // User accept the certificate
                        viewModel.handle(OnboardingAction.UserAcceptCertificate(cause.fingerprint, event.retryAction))
                    }

                    override fun onIgnore() {
                        // Cannot happen in this case
                    }

                    override fun onReject() {
                        // Nothing to do in this case
                    }
                })
    }

    open fun onError(throwable: Throwable) {
        super.showFailure(throwable)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return when {
            displayCancelDialog && viewModel.isRegistrationStarted && backIsHardExit() -> {
                // Ask for confirmation before cancelling the registration
                MaterialAlertDialogBuilder(requireActivity())
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
            displayCancelDialog && isResetPasswordStarted -> {
                // Ask for confirmation before cancelling the reset password
                MaterialAlertDialogBuilder(requireActivity())
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
            else -> {
                resetViewModel()
                // Do not consume the Back event
                false
            }
        }
    }

    final override fun invalidate() = withState(viewModel) { state ->
        // True when email is sent with success to the homeserver
        isResetPasswordStarted = state.resetState.email.isNullOrBlank().not()

        updateWithState(state)
    }

    open fun updateWithState(state: OnboardingViewState) {
        // No op by default
    }

    // Reset any modification on the viewModel by the current fragment
    abstract fun resetViewModel()
}
