/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.ui.consent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentAnalyticsOptinBinding
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class AnalyticsOptInFragment :
        VectorBaseFragment<FragmentAnalyticsOptinBinding>(),
        OnBackPressed {

    @Inject lateinit var analyticsConfig: AnalyticsConfig

    // Share the view model with the Activity so that the Activity
    // can decide what to do when the data has been saved
    private val viewModel: AnalyticsConsentViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAnalyticsOptinBinding {
        return FragmentAnalyticsOptinBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLink()
        setupListeners()
    }

    private fun setupListeners() {
        views.submit.debouncedClicks {
            viewModel.handle(AnalyticsConsentViewActions.SetUserConsent(userConsent = true))
        }
        views.later.debouncedClicks {
            viewModel.handle(AnalyticsConsentViewActions.SetUserConsent(userConsent = false))
        }
    }

    private fun setupLink() {
        views.subtitle.setTextWithColoredPart(
                fullTextRes = CommonStrings.analytics_opt_in_content,
                coloredTextRes = CommonStrings.analytics_opt_in_content_link,
                onClick = {
                    openUrlInChromeCustomTab(requireContext(), null, analyticsConfig.policyLink)
                }
        )
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        // Consider user does not give consent
        viewModel.handle(AnalyticsConsentViewActions.SetUserConsent(userConsent = false))
        // And consume the event
        return true
    }
}
