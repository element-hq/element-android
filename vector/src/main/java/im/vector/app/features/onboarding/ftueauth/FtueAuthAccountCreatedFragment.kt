/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.animations.play
import im.vector.app.core.utils.isAnimationEnabled
import im.vector.app.core.utils.styleMatchingText
import im.vector.app.databinding.FragmentFtueAccountCreatedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class FtueAuthAccountCreatedFragment :
        AbstractFtueAuthFragment<FragmentFtueAccountCreatedBinding>() {

    private var hasPlayedConfetti = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueAccountCreatedBinding {
        return FragmentFtueAccountCreatedBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.accountCreatedPersonalize.debouncedClicks { viewModel.handle(OnboardingAction.PersonalizeProfile) }
        views.accountCreatedTakeMeHome.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome)) }
        views.accountCreatedTakeMeHomeCta.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome)) }
    }

    override fun updateWithState(state: OnboardingViewState) {
        val userId = state.personalizationState.userId
        val subtitle = getString(CommonStrings.ftue_account_created_subtitle, userId).toSpannable().styleMatchingText(userId, Typeface.BOLD)
        views.accountCreatedSubtitle.text = subtitle
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
