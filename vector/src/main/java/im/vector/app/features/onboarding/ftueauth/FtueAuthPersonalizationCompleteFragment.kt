/*
 * Copyright 2021-2024 New Vector Ltd.
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
import im.vector.app.databinding.FragmentFtuePersonalizationCompleteBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents

@AndroidEntryPoint
class FtueAuthPersonalizationCompleteFragment :
        AbstractFtueAuthFragment<FragmentFtuePersonalizationCompleteBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtuePersonalizationCompleteBinding {
        return FragmentFtuePersonalizationCompleteBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.personalizationCompleteCta.debouncedClicks { viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome)) }
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome))
        return true
    }
}
