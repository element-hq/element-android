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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.FragmentLoginResetPasswordMailConfirmationBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import org.matrix.android.sdk.api.failure.is401
import javax.inject.Inject

/**
 * In this screen, the user is asked to check his email and to click on a button once it's done
 */
class FtueAuthResetPasswordMailConfirmationFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentLoginResetPasswordMailConfirmationBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordMailConfirmationBinding {
        return FragmentLoginResetPasswordMailConfirmationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.resetPasswordMailConfirmationSubmit.setOnClickListener { submit() }
    }

    private fun setupUi(state: OnboardingViewState) {
        views.resetPasswordMailConfirmationNotice.text = getString(R.string.login_reset_password_mail_confirmation_notice, state.resetPasswordEmail)
    }

    private fun submit() {
        viewModel.handle(OnboardingAction.ResetPasswordMailConfirmed)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }

    override fun updateWithState(state: OnboardingViewState) {
        setupUi(state)
    }

    override fun onError(throwable: Throwable) {
        // Link in email not yet clicked ?
        val message = if (throwable.is401()) {
            getString(R.string.auth_reset_password_error_unauthorized)
        } else {
            errorFormatter.toHumanReadable(throwable)
        }

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }
}
