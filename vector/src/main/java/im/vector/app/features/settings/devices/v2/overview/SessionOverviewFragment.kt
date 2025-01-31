/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.databinding.FragmentSessionOverviewBinding
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import javax.inject.Inject

/**
 * Display the overview info about a Session.
 */
@AndroidEntryPoint
class SessionOverviewFragment :
        VectorBaseFragment<FragmentSessionOverviewBinding>() {

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
        initSessionInfoView()
    }

    private fun initSessionInfoView() {
        views.sessionOverviewInfo.onLearnMoreClickListener = {
            Toast.makeText(context, "Learn more verification status", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        cleanUpSessionInfoView()
        super.onDestroyView()
    }

    private fun cleanUpSessionInfoView() {
        views.sessionOverviewInfo.onLearnMoreClickListener = null
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateToolbar(state.isCurrentSession)
        updateEntryDetails(state.deviceId)
        if (state.deviceInfo is Success) {
            renderSessionInfo(state.isCurrentSession, state.deviceInfo.invoke())
        } else {
            hideSessionInfo()
        }
    }

    private fun updateToolbar(isCurrentSession: Boolean) {
        val titleResId = if (isCurrentSession) R.string.device_manager_current_session_title else R.string.device_manager_session_title
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(titleResId)
    }

    private fun updateEntryDetails(deviceId: String) {
        views.sessionOverviewEntryDetails.setOnClickListener {
            viewNavigator.navigateToSessionDetails(requireContext(), deviceId)
        }
    }

    private fun renderSessionInfo(isCurrentSession: Boolean, deviceFullInfo: DeviceFullInfo) {
        views.sessionOverviewInfo.isVisible = true
        val viewState = SessionInfoViewState(
                isCurrentSession = isCurrentSession,
                deviceFullInfo = deviceFullInfo,
                isDetailsButtonVisible = false,
                isLearnMoreLinkVisible = true,
                isLastSeenDetailsVisible = true,
        )
        views.sessionOverviewInfo.render(viewState, dateFormatter, drawableProvider, colorProvider)
    }

    private fun hideSessionInfo() {
        views.sessionOverviewInfo.isGone = true
    }
}
