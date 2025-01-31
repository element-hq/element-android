/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Fail
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.databinding.FragmentLoginResetPasswordMailConfirmationBinding
import org.matrix.android.sdk.api.failure.is401

/**
 * In this screen, the user is asked to check their email and to click on a button once it's done.
 */
@AndroidEntryPoint
class LoginResetPasswordMailConfirmationFragment :
        AbstractLoginFragment<FragmentLoginResetPasswordMailConfirmationBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordMailConfirmationBinding {
        return FragmentLoginResetPasswordMailConfirmationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.resetPasswordMailConfirmationSubmit.debouncedClicks { submit() }
    }

    private fun setupUi(state: LoginViewState) {
        views.resetPasswordMailConfirmationNotice.text = getString(R.string.login_reset_password_mail_confirmation_notice, state.resetPasswordEmail)
    }

    private fun submit() {
        loginViewModel.handle(LoginAction.ResetPasswordMailConfirmed)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)

        when (state.asyncResetMailConfirmed) {
            is Fail -> {
                // Link in email not yet clicked ?
                val message = if (state.asyncResetMailConfirmed.error.is401()) {
                    getString(R.string.auth_reset_password_error_unauthorized)
                } else {
                    errorFormatter.toHumanReadable(state.asyncResetMailConfirmed.error)
                }

                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
            else -> Unit
        }
    }
}
