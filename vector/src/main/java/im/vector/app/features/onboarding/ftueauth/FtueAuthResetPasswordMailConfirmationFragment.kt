/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentLoginResetPasswordMailConfirmationBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.is401

/**
 * In this screen, the user is asked to check their email and to click on a button once it's done.
 */
@AndroidEntryPoint
class FtueAuthResetPasswordMailConfirmationFragment :
        AbstractFtueAuthFragment<FragmentLoginResetPasswordMailConfirmationBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordMailConfirmationBinding {
        return FragmentLoginResetPasswordMailConfirmationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.resetPasswordMailConfirmationSubmit.setOnClickListener { submit() }
    }

    private fun setupUi(state: OnboardingViewState) {
        views.resetPasswordMailConfirmationNotice.text = getString(CommonStrings.login_reset_password_mail_confirmation_notice, state.resetState.email)
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
            getString(CommonStrings.auth_reset_password_error_unauthorized)
        } else {
            errorFormatter.toHumanReadable(throwable)
        }

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(message)
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }
}
