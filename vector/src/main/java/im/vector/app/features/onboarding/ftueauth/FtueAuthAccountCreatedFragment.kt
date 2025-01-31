/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.animations.play
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.utils.isAnimationEnabled
import im.vector.app.core.utils.styleMatchingText
import im.vector.app.databinding.FragmentFtueAccountCreatedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthAccountCreatedFragment :
        AbstractFtueAuthFragment<FragmentFtueAccountCreatedBinding>() {

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

    private var hasPlayedConfetti = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueAccountCreatedBinding {
        return FragmentFtueAccountCreatedBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val userId = activeSessionHolder.getActiveSession().myUserId
        val subtitle = getString(R.string.ftue_account_created_subtitle, userId).toSpannable().styleMatchingText(userId, Typeface.BOLD)
        views.accountCreatedSubtitle.text = subtitle
        views.accountCreatedPersonalize.debouncedClicks { viewModel.handle(OnboardingAction.PersonalizeProfile) }
        views.accountCreatedTakeMeHome.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome)) }
        views.accountCreatedTakeMeHomeCta.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome)) }
    }

    override fun updateWithState(state: OnboardingViewState) {
        val canPersonalize = state.personalizationState.supportsPersonalization()
        views.personalizeButtonGroup.isVisible = canPersonalize
        views.takeMeHomeButtonGroup.isVisible = !canPersonalize

        if (!hasPlayedConfetti && requireContext().isAnimationEnabled()) {
            hasPlayedConfetti = true
            views.viewKonfetti.isVisible = true
            views.viewKonfetti.play()
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome))
        return true
    }
}
