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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginResetPasswordBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.isEmail
import reactivecircus.flowbinding.android.widget.textChanges

/**
 * In this screen, the user is asked for email and new password to reset his password.
 */
@AndroidEntryPoint
class FtueAuthResetPasswordFragment :
        AbstractFtueAuthFragment<FragmentLoginResetPasswordBinding>() {

    // Show warning only once
    private var showWarning = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordBinding {
        return FragmentLoginResetPasswordBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSubmitButton()
    }

    override fun onError(throwable: Throwable) {
        views.resetPasswordEmailTil.error = errorFormatter.toHumanReadable(throwable)
    }

    private fun setupUi(state: OnboardingViewState) {
        views.resetPasswordTitle.text = getString(CommonStrings.login_reset_password_on, state.selectedHomeserver.userFacingUrl.toReducedUrl())
    }

    private fun setupSubmitButton() {
        views.resetPasswordSubmit.setOnClickListener { submit() }
        combine(
                views.resetPasswordEmail.textChanges().map { it.isEmail() },
                views.passwordField.textChanges().map { it.isNotEmpty() }
        ) { isEmail, isPasswordNotEmpty ->
            isEmail && isPasswordNotEmpty
        }
                .onEach {
                    views.resetPasswordEmailTil.error = null
                    views.passwordFieldTil.error = null
                    views.resetPasswordSubmit.isEnabled = it
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun submit() {
        cleanupUi()

        if (showWarning) {
            showWarning = false
            // Display a warning as Riot-Web does first
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(CommonStrings.login_reset_password_warning_title)
                    .setMessage(CommonStrings.login_reset_password_warning_content)
                    .setPositiveButton(CommonStrings.login_reset_password_warning_submit) { _, _ ->
                        doSubmit()
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        } else {
            doSubmit()
        }
    }

    private fun doSubmit() {
        val email = views.resetPasswordEmail.text.toString()
        val password = views.passwordField.text.toString()

        viewModel.handle(OnboardingAction.ResetPassword(email, password))
    }

    private fun cleanupUi() {
        views.resetPasswordSubmit.hideKeyboard()
        views.resetPasswordEmailTil.error = null
        views.passwordFieldTil.error = null
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }

    override fun updateWithState(state: OnboardingViewState) {
        setupUi(state)
        if (state.isLoading) {
            // Ensure new password is hidden
            views.passwordField.hidePassword()
        }
    }
}
