/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.dialogs.ManuallyVerifyDialog
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextColor
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentSettingsDevicesBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.list.NUMBER_OF_OTHER_DEVICES_TO_RENDER
import im.vector.app.features.settings.devices.v2.list.OtherSessionsView
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
import im.vector.app.features.settings.devices.v2.list.SecurityRecommendationView
import im.vector.app.features.settings.devices.v2.list.SecurityRecommendationViewState
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import im.vector.app.features.settings.devices.v2.signout.BuildConfirmSignoutDialogUseCase
import im.vector.app.features.workers.signout.SignOutUiWorker
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.extensions.orFalse
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

    @Inject lateinit var vectorFeatures: VectorFeatures

    @Inject lateinit var stringProvider: StringProvider

    @Inject lateinit var buildConfirmSignoutDialogUseCase: BuildConfirmSignoutDialogUseCase

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
                ?.setTitle(CommonStrings.settings_sessions_list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initWaitingView()
        initCurrentSessionHeaderView()
        initCurrentSessionView()
        initOtherSessionsHeaderView()
        initOtherSessionsView()
        initSecurityRecommendationsView()
        observeViewEvents()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is DevicesViewEvent.RequestReAuth -> askForReAuthentication(it)
                is DevicesViewEvent.ShowVerifyDevice -> {
                    // TODO selfverif
//                    VerificationBottomSheet.withArgs(
// //                            roomId = null,
//                            otherUserId = it.userId,
//                            transactionId = it.transactionId ?:""
//                    ).show(childFragmentManager, "REQPOP")
                }
                is DevicesViewEvent.SelfVerification -> {
                    navigator.requestSelfSessionVerification(requireActivity())
                }
                is DevicesViewEvent.ShowManuallyVerify -> {
                    ManuallyVerifyDialog.show(requireActivity(), it.cryptoDeviceInfo) {
                        viewModel.handle(DevicesAction.MarkAsManuallyVerified(it.cryptoDeviceInfo))
                    }
                }
                is DevicesViewEvent.PromptResetSecrets -> {
                    navigator.open4SSetup(requireActivity(), SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
                }
                is DevicesViewEvent.SignoutError -> showFailure(it.error)
                is DevicesViewEvent.SignoutSuccess -> Unit // do nothing
            }
        }
    }

    private fun initWaitingView() {
        views.waitingView.waitingStatusText.setText(CommonStrings.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
    }

    private fun initCurrentSessionHeaderView() {
        views.deviceListHeaderCurrentSession.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.currentSessionHeaderRename -> {
                    navigateToRenameCurrentSession()
                    true
                }
                R.id.currentSessionHeaderSignout -> {
                    confirmSignoutCurrentSession()
                    true
                }
                R.id.currentSessionHeaderSignoutOtherSessions -> {
                    confirmMultiSignoutOtherSessions()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToRenameCurrentSession() = withState(viewModel) { state ->
        val currentDeviceId = state.currentSessionCrossSigningInfo.deviceId
        if (currentDeviceId.isNotEmpty()) {
            viewNavigator.navigateToRenameSession(
                    context = requireActivity(),
                    deviceId = currentDeviceId,
            )
        }
    }

    private fun confirmSignoutCurrentSession() {
        activity?.let { SignOutUiWorker(it).perform() }
    }

    private fun initCurrentSessionView() {
        views.deviceListCurrentSession.viewVerifyButton.debouncedClicks {
            viewModel.handle(DevicesAction.VerifyCurrentSession)
        }
    }

    private fun initOtherSessionsHeaderView() {
        views.deviceListHeaderOtherSessions.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.otherSessionsHeaderMultiSignout -> {
                    confirmMultiSignoutOtherSessions()
                    true
                }
                R.id.otherSessionsHeaderToggleIpAddress -> {
                    handleToggleIpAddressVisibility()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleToggleIpAddressVisibility() {
        viewModel.handle(DevicesAction.ToggleIpAddressVisibility)
    }

    private fun confirmMultiSignoutOtherSessions() {
        activity?.let {
            buildConfirmSignoutDialogUseCase.execute(it, this::multiSignoutOtherSessions)
                    .show()
        }
    }

    private fun multiSignoutOtherSessions() {
        viewModel.handle(DevicesAction.MultiSignoutOtherSessions)
    }

    private fun initOtherSessionsView() {
        views.deviceListOtherSessions.callback = this
    }

    private fun initSecurityRecommendationsView() {
        views.deviceListUnverifiedSessionsRecommendation.callback = object : SecurityRecommendationView.Callback {
            override fun onViewAllClicked() {
                viewNavigator.navigateToOtherSessions(
                        requireActivity(),
                        DeviceManagerFilterType.UNVERIFIED,
                        excludeCurrentDevice = true
                )
            }
        }
        views.deviceListInactiveSessionsRecommendation.callback = object : SecurityRecommendationView.Callback {
            override fun onViewAllClicked() {
                viewNavigator.navigateToOtherSessions(
                        requireActivity(),
                        DeviceManagerFilterType.INACTIVE,
                        excludeCurrentDevice = true
                )
            }
        }
    }

    override fun onDestroyView() {
        cleanUpLearnMoreButtonsListeners()
        super.onDestroyView()
    }

    private fun cleanUpLearnMoreButtonsListeners() {
        views.deviceListHeaderOtherSessions.onLearnMoreClickListener = null
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.devices is Success) {
            val deviceFullInfoList = state.devices()
            val devices = deviceFullInfoList?.allSessions
            val currentDeviceId = state.currentSessionCrossSigningInfo.deviceId
            val currentDeviceInfo = devices?.firstOrNull { it.deviceInfo.deviceId == currentDeviceId }
            val otherDevices = devices?.filter { it.deviceInfo.deviceId != currentDeviceId }
            val inactiveSessionsCount = deviceFullInfoList?.inactiveSessionsCount ?: 0
            val unverifiedSessionsCount = deviceFullInfoList?.unverifiedSessionsCount ?: 0

            renderSecurityRecommendations(inactiveSessionsCount, unverifiedSessionsCount)
            renderCurrentSessionView(currentDeviceInfo, hasOtherDevices = otherDevices?.isNotEmpty().orFalse(), state)
            renderOtherSessionsView(otherDevices, state)
        } else {
            hideSecurityRecommendations()
            hideCurrentSessionView()
            hideOtherSessionsView()
        }

        handleLoadingStatus(state.isLoading)
    }

    private fun renderSecurityRecommendations(
            inactiveSessionsCount: Int,
            unverifiedSessionsCount: Int,
    ) {
        val isUnverifiedSectionVisible = unverifiedSessionsCount > 0
        val isInactiveSectionVisible = inactiveSessionsCount > 0
        if (isUnverifiedSectionVisible.not() && isInactiveSectionVisible.not()) {
            hideSecurityRecommendations()
        } else {
            views.deviceListHeaderSectionSecurityRecommendations.isVisible = true
            views.deviceListSecurityRecommendationsDivider.isVisible = true

            views.deviceListUnverifiedSessionsRecommendation.isVisible = isUnverifiedSectionVisible
            views.deviceListInactiveSessionsRecommendation.isVisible = isInactiveSectionVisible
            val unverifiedSessionsViewState = SecurityRecommendationViewState(
                    description = getString(CommonStrings.device_manager_unverified_sessions_description),
                    sessionsCount = unverifiedSessionsCount,
            )
            views.deviceListUnverifiedSessionsRecommendation.render(unverifiedSessionsViewState)
            val inactiveSessionsViewState = SecurityRecommendationViewState(
                    description = resources.getQuantityString(
                            CommonPlurals.device_manager_inactive_sessions_description,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
                    ),
                    sessionsCount = inactiveSessionsCount,
            )
            views.deviceListInactiveSessionsRecommendation.render(inactiveSessionsViewState)
        }
    }

    private fun hideUnverifiedSessionsRecommendation() {
        views.deviceListUnverifiedSessionsRecommendation.isVisible = false
    }

    private fun hideInactiveSessionsRecommendation() {
        views.deviceListInactiveSessionsRecommendation.isVisible = false
    }

    private fun hideSecurityRecommendations() {
        views.deviceListHeaderSectionSecurityRecommendations.isVisible = false
        views.deviceListSecurityRecommendationsDivider.isVisible = false
        hideUnverifiedSessionsRecommendation()
        hideInactiveSessionsRecommendation()
    }

    private fun renderOtherSessionsView(otherDevices: List<DeviceFullInfo>?, state: DevicesViewState) {
        val isShowingIpAddress = state.isShowingIpAddress
        if (otherDevices.isNullOrEmpty()) {
            hideOtherSessionsView()
        } else {
            views.deviceListHeaderOtherSessions.isVisible = true
            val colorDestructive = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError)
            val multiSignoutItem = views.deviceListHeaderOtherSessions.menu.findItem(R.id.otherSessionsHeaderMultiSignout)
            // Hide multi signout if the homeserver delegates the account management
            multiSignoutItem.isVisible = state.delegatedOidcAuthEnabled.not()
            val nbDevices = otherDevices.size
            multiSignoutItem.title = stringProvider.getQuantityString(CommonPlurals.device_manager_other_sessions_multi_signout_all, nbDevices, nbDevices)
            multiSignoutItem.setTextColor(colorDestructive)
            views.deviceListOtherSessions.isVisible = true
            val devices = if (isShowingIpAddress) otherDevices else otherDevices.map { it.copy(deviceInfo = it.deviceInfo.copy(lastSeenIp = null)) }
            views.deviceListOtherSessions.render(
                    devices = devices.take(NUMBER_OF_OTHER_DEVICES_TO_RENDER),
                    totalNumberOfDevices = devices.size,
                    showViewAll = devices.size > NUMBER_OF_OTHER_DEVICES_TO_RENDER
            )
            views.deviceListHeaderOtherSessions.menu.findItem(R.id.otherSessionsHeaderToggleIpAddress).title = if (isShowingIpAddress) {
                stringProvider.getString(CommonStrings.device_manager_other_sessions_hide_ip_address)
            } else {
                stringProvider.getString(CommonStrings.device_manager_other_sessions_show_ip_address)
            }
        }
    }

    private fun hideOtherSessionsView() {
        views.deviceListHeaderOtherSessions.isVisible = false
        views.deviceListOtherSessions.isVisible = false
    }

    private fun renderCurrentSessionView(currentDeviceInfo: DeviceFullInfo?, hasOtherDevices: Boolean, state: DevicesViewState) {
        currentDeviceInfo?.let {
            renderCurrentSessionHeaderView(hasOtherDevices, state)
            renderCurrentSessionListView(it)
        } ?: run {
            hideCurrentSessionView()
        }
    }

    private fun renderCurrentSessionHeaderView(hasOtherDevices: Boolean, state: DevicesViewState) {
        views.deviceListHeaderCurrentSession.isVisible = true
        val colorDestructive = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError)
        val signoutSessionItem = views.deviceListHeaderCurrentSession.menu.findItem(R.id.currentSessionHeaderSignout)
        signoutSessionItem.setTextColor(colorDestructive)
        val signoutOtherSessionsItem = views.deviceListHeaderCurrentSession.menu.findItem(R.id.currentSessionHeaderSignoutOtherSessions)
        signoutOtherSessionsItem.setTextColor(colorDestructive)
        // Hide signout other sessions if the homeserver delegates the account management
        signoutOtherSessionsItem.isVisible = hasOtherDevices && state.delegatedOidcAuthEnabled.not()
    }

    private fun renderCurrentSessionListView(currentDeviceInfo: DeviceFullInfo) {
        views.deviceListCurrentSession.isVisible = true
        val viewState = SessionInfoViewState(
                isCurrentSession = true,
                deviceFullInfo = currentDeviceInfo
        )
        views.deviceListCurrentSession.render(viewState, dateFormatter, drawableProvider, colorProvider, stringProvider)
        views.deviceListCurrentSession.debouncedClicks {
            currentDeviceInfo.deviceInfo.deviceId?.let { deviceId -> navigateToSessionOverview(deviceId) }
        }
        views.deviceListCurrentSession.viewDetailsButton.debouncedClicks {
            currentDeviceInfo.deviceInfo.deviceId?.let { deviceId -> navigateToSessionOverview(deviceId) }
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

    override fun onOtherSessionLongClicked(deviceId: String) {
        // do nothing
    }

    override fun onOtherSessionClicked(deviceId: String) {
        navigateToSessionOverview(deviceId)
    }

    override fun onViewAllOtherSessionsClicked() {
        viewNavigator.navigateToOtherSessions(
                context = requireActivity(),
                defaultFilter = DeviceManagerFilterType.ALL_SESSIONS,
                excludeCurrentDevice = true
        )
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(DevicesAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(DevicesAction.PasswordAuthDone(password))
                }
                else -> {
                    viewModel.handle(DevicesAction.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(DevicesAction.ReAuthCancelled)
        }
    }

    /**
     * Launch the re auth activity to get credentials.
     */
    private fun askForReAuthentication(reAuthReq: DevicesViewEvent.RequestReAuth) {
        ReAuthActivity.newIntent(
                requireContext(),
                reAuthReq.registrationFlowResponse,
                reAuthReq.lastErrorCode,
                getString(CommonStrings.devices_delete_dialog_title)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }
}
