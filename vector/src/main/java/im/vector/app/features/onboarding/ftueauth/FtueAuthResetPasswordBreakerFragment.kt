/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.utils.colorTerminatingFullStop
import im.vector.app.databinding.FragmentFtueResetPasswordBreakerBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.themes.ThemeProvider
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class FtueAuthResetPasswordBreakerArgument(
        val email: String
) : Parcelable

@AndroidEntryPoint
class FtueAuthResetPasswordBreakerFragment :
        AbstractFtueAuthFragment<FragmentFtueResetPasswordBreakerBinding>() {

    @Inject lateinit var themeProvider: ThemeProvider
    private val params: FtueAuthResetPasswordBreakerArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueResetPasswordBreakerBinding {
        return FragmentFtueResetPasswordBreakerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        views.resetPasswordBreakerGradientContainer.setBackgroundResource(themeProvider.ftueBreakerBackground())
        views.resetPasswordBreakerTitle.text = getString(CommonStrings.ftue_auth_reset_password_breaker_title)
                .colorTerminatingFullStop(ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorSecondary))
        views.resetPasswordBreakerSubtitle.text = getString(CommonStrings.ftue_auth_password_reset_email_confirmation_subtitle, params.email)
        views.resetPasswordBreakerResendEmail.debouncedClicks { viewModel.handle(OnboardingAction.ResendResetPassword) }
        views.resetPasswordBreakerFooter.debouncedClicks {
            viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnResetPasswordBreakerConfirmed))
        }
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }
}
