/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.analytics.ui.consent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import im.vector.app.R
import im.vector.app.config.analyticsConfig
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentAnalyticsOptinBinding
import javax.inject.Inject

class AnalyticsOptInFragment @Inject constructor() :
    VectorBaseFragment<FragmentAnalyticsOptinBinding>(),
        OnBackPressed {

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
                fullTextRes = R.string.analytics_opt_in_content,
                coloredTextRes = R.string.analytics_opt_in_content_link,
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
