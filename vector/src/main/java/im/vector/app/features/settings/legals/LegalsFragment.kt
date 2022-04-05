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
package im.vector.app.features.settings.legals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.core.utils.displayInWebView
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.settings.VectorSettingsUrls
import im.vector.app.openOssLicensesMenuActivity
import javax.inject.Inject

class LegalsFragment @Inject constructor(
        private val controller: LegalsController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        LegalsController.Listener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(LegalsViewModel::class)
    private val firstThrottler = FirstThrottler(1000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsLegals
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.listener = this
        views.genericRecyclerView.configureWith(controller)
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.preference_root_legals)
        viewModel.handle(LegalsAction.Refresh)
    }

    override fun onTapRetry() {
        viewModel.handle(LegalsAction.Refresh)
    }

    override fun openPolicy(policy: ServerPolicy) {
        openUrl(policy.url)
    }

    override fun openThirdPartyNotice() {
        openUrl(VectorSettingsUrls.THIRD_PARTY_LICENSES)
    }

    private fun openUrl(url: String) {
        if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
            if (url.startsWith("file://")) {
                activity?.displayInWebView(url)
            } else {
                openUrlInChromeCustomTab(requireContext(), null, url)
            }
        }
    }

    override fun openThirdPartyNoticeGplay() {
        if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
            // See https://developers.google.com/android/guides/opensource
            openOssLicensesMenuActivity(requireActivity())
        }
    }
}
