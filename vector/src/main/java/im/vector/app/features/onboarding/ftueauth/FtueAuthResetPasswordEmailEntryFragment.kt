/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueResetPasswordEmailInputBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.isEmail

@AndroidEntryPoint
class FtueAuthResetPasswordEmailEntryFragment :
        AbstractFtueAuthFragment<FragmentFtueResetPasswordEmailInputBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueResetPasswordEmailInputBinding {
        return FragmentFtueResetPasswordEmailInputBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.emailEntryInput.associateContentStateWith(button = views.emailEntrySubmit, enabledPredicate = { it.isEmail() })
        views.emailEntryInput.setOnImeDoneListener { startPasswordReset() }
        views.emailEntryInput.clearErrorOnChange(viewLifecycleOwner)
        views.emailEntrySubmit.debouncedClicks { startPasswordReset() }
    }

    private fun startPasswordReset() {
        val email = views.emailEntryInput.content()
        viewModel.handle(OnboardingAction.ResetPassword(email = email, newPassword = null))
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.emailEntryHeaderSubtitle.text = getString(
                CommonStrings.ftue_auth_reset_password_email_subtitle,
                state.selectedHomeserver.userFacingUrl.toReducedUrl()
        )
    }

    override fun onError(throwable: Throwable) {
        views.emailEntryInput.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }
}
