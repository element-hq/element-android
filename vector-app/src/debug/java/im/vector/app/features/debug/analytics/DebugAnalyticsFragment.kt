/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.toOnOff
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.application.databinding.FragmentDebugAnalyticsBinding
import me.gujun.android.span.span

class DebugAnalyticsFragment : VectorBaseFragment<FragmentDebugAnalyticsBinding>() {

    private val viewModel: DebugAnalyticsViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDebugAnalyticsBinding {
        return FragmentDebugAnalyticsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewListeners()
    }

    private fun setViewListeners() {
        views.showAnalyticsOptIn.onClick {
            navigator.openAnalyticsOptIn(requireContext())
        }
        views.resetAnalyticsOptInDisplayed.onClick {
            viewModel.handle(DebugAnalyticsViewActions.ResetAnalyticsOptInDisplayed)
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        views.analyticsStoreContent.text = span {
            +"AnalyticsId: "
            span {
                textStyle = "bold"
                text = state.analyticsId.orEmpty()
            }
            +"\nOptIn: "
            span {
                textStyle = "bold"
                text = state.userConsent.toOnOff()
            }
            +"\nDidAsk: "
            span {
                textStyle = "bold"
                text = state.didAskUserConsent.toString()
            }
        }
    }
}
