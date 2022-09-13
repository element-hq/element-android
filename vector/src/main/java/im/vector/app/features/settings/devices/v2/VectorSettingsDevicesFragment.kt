/*
 * Copyright 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
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
import im.vector.app.core.dialogs.ManuallyVerifyDialog
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.databinding.FragmentSettingsDevicesBinding
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.settings.devices.v2.list.NUMBER_OF_OTHER_DEVICES_TO_RENDER
import im.vector.app.features.settings.devices.v2.list.OtherSessionsView
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
import im.vector.app.features.settings.devices.v2.list.SecurityRecommendationViewState
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import javax.inject.Inject

/**
 * Display the list of the user's devices and sessions.
 */
@AndroidEntryPoint
class VectorSettingsDevicesFragment :
        VectorBaseFragment<FragmentSettingsDevicesBinding>(),
        OtherSessionsView.Callback {

    @Inject lateinit var viewNavigator: VectorSettingsDevicesViewNavigator

    @Inject lateinit var dateFormatter: VectorDateFormatter

    @Inject lateinit var drawableProvider: DrawableProvider

    @Inject lateinit var colorProvider: ColorProvider

    private val viewModel: DevicesViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsDevicesBinding {
        return FragmentSettingsDevicesBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initToolbar()
    }

    private fun initToolbar() {
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(R.string.settings_sessions_list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLearnMoreButtons()
        initWaitingView()
        initOtherSessionsView()
        observeViewEvents()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is DevicesViewEvent.Loading -> showLoading(it.message)
                is DevicesViewEvent.Failure -> showFailure(it.throwable)
                is DevicesViewEvent.RequestReAuth -> Unit // TODO. Next PR
                is DevicesViewEvent.PromptRenameDevice -> Unit // TODO. Next PR
                is DevicesViewEvent.ShowVerifyDevice -> {
                    VerificationBottomSheet.withArgs(
                            roomId = null,
                            otherUserId = it.userId,
                            transactionId = it.transactionId
                    ).show(childFragmentManager, "REQPOP")
                }
                is DevicesViewEvent.SelfVerification -> {
                    VerificationBottomSheet.forSelfVerification(it.session)
                            .show(childFragmentManager, "REQPOP")
                }
                is DevicesViewEvent.ShowManuallyVerify -> {
                    ManuallyVerifyDialog.show(requireActivity(), it.cryptoDeviceInfo) {
                        viewModel.handle(DevicesAction.MarkAsManuallyVerified(it.cryptoDeviceInfo))
                    }
                }
                is DevicesViewEvent.PromptResetSecrets -> {
                    navigator.open4SSetup(requireActivity(), SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
                }
            }
        }
    }

    private fun initWaitingView() {
        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
    }

    private fun initOtherSessionsView() {
        views.deviceListOtherSessions.callback = this
    }

    override fun onDestroyView() {
        cleanUpLearnMoreButtonsListeners()
        super.onDestroyView()
    }

    private fun initLearnMoreButtons() {
        views.deviceListHeaderOtherSessions.onLearnMoreClickListener = {
            Toast.makeText(context, "Learn more other", Toast.LENGTH_LONG).show()
        }
    }

    private fun cleanUpLearnMoreButtonsListeners() {
        views.deviceListHeaderOtherSessions.onLearnMoreClickListener = null
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.devices is Success) {
            val devices = state.devices()
            val currentDeviceId = state.currentSessionCrossSigningInfo.deviceId
            val currentDeviceInfo = devices?.firstOrNull {
                it.deviceInfo.deviceId == currentDeviceId
            }
            val otherDevices = devices?.filter { it.deviceInfo.deviceId != currentDeviceId }

            renderSecurityRecommendations(state.inactiveSessionsCount, state.unverifiedSessionsCount)
            renderCurrentDevice(currentDeviceInfo)
            renderOtherSessionsView(otherDevices)
        } else {
            hideSecurityRecommendations()
            hideCurrentSessionView()
            hideOtherSessionsView()
        }

        handleLoadingStatus(state.isLoading)
    }

    private fun renderSecurityRecommendations(inactiveSessionsCount: Int, unverifiedSessionsCount: Int) {
        if (unverifiedSessionsCount == 0 && inactiveSessionsCount == 0) {
            hideSecurityRecommendations()
        } else {
            views.deviceListHeaderSectionSecurityRecommendations.isVisible = true
            views.deviceListSecurityRecommendationsDivider.isVisible = true
            views.deviceListUnverifiedSessionsRecommendation.isVisible = unverifiedSessionsCount > 0
            views.deviceListInactiveSessionsRecommendation.isVisible = inactiveSessionsCount > 0
            val unverifiedSessionsViewState = SecurityRecommendationViewState(
                    description = getString(R.string.device_manager_unverified_sessions_description),
                    sessionsCount = unverifiedSessionsCount,
            )
            views.deviceListUnverifiedSessionsRecommendation.render(unverifiedSessionsViewState)
            val inactiveSessionsViewState = SecurityRecommendationViewState(
                    description = resources.getQuantityString(
                            R.plurals.device_manager_inactive_sessions_description,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
                    ),
                    sessionsCount = inactiveSessionsCount,
            )
            views.deviceListInactiveSessionsRecommendation.render(inactiveSessionsViewState)
        }
    }

    private fun hideSecurityRecommendations() {
        views.deviceListHeaderSectionSecurityRecommendations.isVisible = false
        views.deviceListUnverifiedSessionsRecommendation.isVisible = false
        views.deviceListInactiveSessionsRecommendation.isVisible = false
        views.deviceListSecurityRecommendationsDivider.isVisible = false
    }

    private fun renderOtherSessionsView(otherDevices: List<DeviceFullInfo>?) {
        if (otherDevices.isNullOrEmpty()) {
            hideOtherSessionsView()
        } else {
            views.deviceListHeaderOtherSessions.isVisible = true
            views.deviceListOtherSessions.isVisible = true
            views.deviceListOtherSessions.render(
                    devices = otherDevices.take(NUMBER_OF_OTHER_DEVICES_TO_RENDER),
                    totalNumberOfDevices = otherDevices.size,
                    showViewAll = otherDevices.size > NUMBER_OF_OTHER_DEVICES_TO_RENDER
            )
        }
    }

    private fun hideOtherSessionsView() {
        views.deviceListHeaderOtherSessions.isVisible = false
        views.deviceListOtherSessions.isVisible = false
    }

    private fun renderCurrentDevice(currentDeviceInfo: DeviceFullInfo?) {
        currentDeviceInfo?.let {
            views.deviceListHeaderCurrentSession.isVisible = true
            views.deviceListCurrentSession.isVisible = true
            val viewState = SessionInfoViewState(
                    isCurrentSession = true,
                    deviceFullInfo = it
            )
            views.deviceListCurrentSession.render(viewState, dateFormatter, drawableProvider, colorProvider)
            views.deviceListCurrentSession.debouncedClicks {
                currentDeviceInfo.deviceInfo.deviceId?.let { deviceId -> navigateToSessionOverview(deviceId) }
            }
            views.deviceListCurrentSession.viewDetailsButton.debouncedClicks {
                currentDeviceInfo.deviceInfo.deviceId?.let { deviceId -> navigateToSessionOverview(deviceId) }
            }
        } ?: run {
            hideCurrentSessionView()
        }
    }

    private fun navigateToSessionOverview(deviceId: String) {
        viewNavigator.navigateToSessionOverview(
                context = requireActivity(),
                deviceId = deviceId
        )
    }

    private fun hideCurrentSessionView() {
        views.deviceListHeaderCurrentSession.isVisible = false
        views.deviceListCurrentSession.isVisible = false
        views.deviceListDividerCurrentSession.isVisible = false
        views.deviceListCurrentSession.debouncedClicks {
            // do nothing
        }
        views.deviceListCurrentSession.viewDetailsButton.debouncedClicks {
            // do nothing
        }
    }

    private fun handleLoadingStatus(isLoading: Boolean) {
        views.waitingView.root.isVisible = isLoading
    }

    override fun onOtherSessionClicked(deviceId: String) {
        navigateToSessionOverview(deviceId)
    }

    override fun onViewAllOtherSessionsClicked() {
        viewNavigator.navigateToOtherSessions(requireActivity())
    }
}
