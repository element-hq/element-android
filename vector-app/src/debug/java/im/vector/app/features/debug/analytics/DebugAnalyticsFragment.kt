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
import im.vector.app.databinding.FragmentDebugAnalyticsBinding
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
