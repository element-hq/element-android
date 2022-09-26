/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.databinding.FragmentSessionOverviewBinding
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import javax.inject.Inject

/**
 * Display the overview info about a Session.
 */
@AndroidEntryPoint
class SessionOverviewFragment :
        VectorBaseFragment<FragmentSessionOverviewBinding>(),
        VectorMenuProvider {

    @Inject lateinit var viewNavigator: SessionOverviewViewNavigator

    @Inject lateinit var dateFormatter: VectorDateFormatter

    @Inject lateinit var drawableProvider: DrawableProvider

    @Inject lateinit var colorProvider: ColorProvider

    private val viewModel: SessionOverviewViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSessionOverviewBinding {
        return FragmentSessionOverviewBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        initSessionInfoView()
        initVerifyButton()
    }

    private fun initSessionInfoView() {
        views.sessionOverviewInfo.onLearnMoreClickListener = {
            Toast.makeText(context, "Learn more verification status", Toast.LENGTH_LONG).show()
        }
    }

    private fun initVerifyButton() {
        views.sessionOverviewInfo.viewVerifyButton.debouncedClicks {
            viewModel.handle(SessionOverviewAction.VerifySession)
        }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is SessionOverviewViewEvent.ShowVerifyCurrentSession -> {
                    navigator.requestSelfSessionVerification(requireActivity())
                }
                is SessionOverviewViewEvent.ShowVerifyOtherSession -> {
                    navigator.requestSessionVerification(requireActivity(), it.deviceId)
                }
                is SessionOverviewViewEvent.PromptResetSecrets -> {
                    navigator.open4SSetup(requireActivity(), SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
                }
            }
        }
    }

    override fun onDestroyView() {
        cleanUpSessionInfoView()
        super.onDestroyView()
    }

    private fun cleanUpSessionInfoView() {
        views.sessionOverviewInfo.onLearnMoreClickListener = null
    }

    override fun getMenuRes() = R.menu.menu_session_overview

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sessionOverviewRename -> {
                goToRenameSession()
                true
            }
            else -> false
        }
    }

    private fun goToRenameSession() = withState(viewModel) { state ->
        viewNavigator.goToRenameSession(requireContext(), state.deviceId)
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateToolbar(state.isCurrentSession)
        updateEntryDetails(state.deviceId)
        updateSessionInfo(state)
    }

    private fun updateToolbar(isCurrentSession: Boolean) {
        val titleResId = if (isCurrentSession) R.string.device_manager_current_session_title else R.string.device_manager_session_title
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(titleResId)
    }

    private fun updateEntryDetails(deviceId: String) {
        views.sessionOverviewEntryDetails.setOnClickListener {
            viewNavigator.goToSessionDetails(requireContext(), deviceId)
        }
    }

    private fun updateSessionInfo(viewState: SessionOverviewViewState) {
        if (viewState.deviceInfo is Success) {
            views.sessionOverviewInfo.isVisible = true
            val isCurrentSession = viewState.isCurrentSession
            val infoViewState = SessionInfoViewState(
                    isCurrentSession = isCurrentSession,
                    deviceFullInfo = viewState.deviceInfo.invoke(),
                    isVerifyButtonVisible = isCurrentSession || viewState.isCurrentSessionTrusted,
                    isDetailsButtonVisible = false,
                    isLearnMoreLinkVisible = true,
                    isLastSeenDetailsVisible = true,
            )
            views.sessionOverviewInfo.render(infoViewState, dateFormatter, drawableProvider, colorProvider)
        } else {
            views.sessionOverviewInfo.isVisible = false
        }
    }
}
