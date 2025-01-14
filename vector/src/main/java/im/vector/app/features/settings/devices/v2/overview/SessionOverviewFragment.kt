/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentSessionOverviewBinding
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import im.vector.app.features.settings.devices.v2.more.SessionLearnMoreBottomSheet
import im.vector.app.features.settings.devices.v2.notification.NotificationsStatus
import im.vector.app.features.settings.devices.v2.signout.BuildConfirmSignoutDialogUseCase
import im.vector.app.features.workers.signout.SignOutUiWorker
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
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

    @Inject lateinit var stringProvider: StringProvider

    @Inject lateinit var buildConfirmSignoutDialogUseCase: BuildConfirmSignoutDialogUseCase

    private val viewModel: SessionOverviewViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSessionOverviewBinding {
        return FragmentSessionOverviewBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        initSessionInfoView()
        initVerifyButton()
        initSignoutButton()
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
                is SessionOverviewViewEvent.RequestReAuth -> askForReAuthentication(it)
                SessionOverviewViewEvent.SignoutSuccess -> viewNavigator.goBack(requireActivity())
                is SessionOverviewViewEvent.SignoutError -> showFailure(it.error)
            }
        }
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

    private fun initSignoutButton() {
        views.sessionOverviewSignout.debouncedClicks {
            confirmSignoutSession()
        }
    }

    private fun confirmSignoutSession() = withState(viewModel) { state ->
        if (state.deviceInfo.invoke()?.isCurrentDevice.orFalse()) {
            confirmSignoutCurrentSession()
        } else {
            confirmSignoutOtherSession()
        }
    }

    private fun confirmSignoutCurrentSession() {
        activity?.let { SignOutUiWorker(it).perform() }
    }

    private fun confirmSignoutOtherSession() = withState(viewModel) { state ->
        if (state.externalAccountManagementUrl != null) {
            // Manage in external account manager
            openUrlInChromeCustomTab(
                    requireContext(),
                    null,
                    state.externalAccountManagementUrl.removeSuffix("/") + "?action=session_end&device_id=${state.deviceId}"
            )
        } else {
            activity?.let {
                buildConfirmSignoutDialogUseCase.execute(it, this::signoutSession)
                        .show()
            }
        }
    }

    private fun signoutSession() {
        viewModel.handle(SessionOverviewAction.SignoutOtherSession)
    }

    override fun onDestroyView() {
        cleanUpSessionInfoView()
        super.onDestroyView()
    }

    private fun cleanUpSessionInfoView() {
        views.sessionOverviewInfo.onLearnMoreClickListener = null
    }

    override fun getMenuRes() = R.menu.menu_session_overview

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            menu.findItem(R.id.sessionOverviewToggleIpAddress).title = if (state.isShowingIpAddress) {
                getString(CommonStrings.device_manager_other_sessions_hide_ip_address)
            } else {
                getString(CommonStrings.device_manager_other_sessions_show_ip_address)
            }
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sessionOverviewRename -> {
                goToRenameSession()
                true
            }
            R.id.sessionOverviewToggleIpAddress -> {
                toggleIpAddressVisibility()
                true
            }
            else -> false
        }
    }

    private fun toggleIpAddressVisibility() {
        viewModel.handle(SessionOverviewAction.ToggleIpAddressVisibility)
    }

    private fun goToRenameSession() = withState(viewModel) { state ->
        viewNavigator.goToRenameSession(requireContext(), state.deviceId)
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateToolbar(state)
        updateEntryDetails(state.deviceId)
        updateSessionInfo(state)
        updateLoading(state.isLoading)
        updatePushNotificationToggle(state.deviceId, state.notificationsStatus)
    }

    private fun updateToolbar(viewState: SessionOverviewViewState) {
        if (viewState.deviceInfo is Success) {
            val titleResId = if (viewState.deviceInfo.invoke().isCurrentDevice) {
                CommonStrings.device_manager_current_session_title
            } else {
                CommonStrings.device_manager_session_title
            }
            (activity as? AppCompatActivity)
                    ?.supportActionBar
                    ?.setTitle(titleResId)
        }
    }

    private fun updateEntryDetails(deviceId: String) {
        views.sessionOverviewEntryDetails.setOnClickListener {
            viewNavigator.goToSessionDetails(requireContext(), deviceId)
        }
    }

    private fun updateSessionInfo(viewState: SessionOverviewViewState) {
        if (viewState.deviceInfo is Success) {
            views.sessionOverviewInfo.isVisible = true
            val deviceInfo = viewState.deviceInfo.invoke()
            val isCurrentSession = deviceInfo.isCurrentDevice
            val infoViewState = SessionInfoViewState(
                    isCurrentSession = isCurrentSession,
                    deviceFullInfo = deviceInfo,
                    isVerifyButtonVisible = isCurrentSession || viewState.isCurrentSessionTrusted,
                    isDetailsButtonVisible = false,
                    isLearnMoreLinkVisible = deviceInfo.roomEncryptionTrustLevel != RoomEncryptionTrustLevel.Default,
                    isLastActivityVisible = !isCurrentSession,
                    isShowingIpAddress = viewState.isShowingIpAddress,
            )
            views.sessionOverviewInfo.render(infoViewState, dateFormatter, drawableProvider, colorProvider, stringProvider)
            views.sessionOverviewInfo.onLearnMoreClickListener = {
                showLearnMoreInfoVerificationStatus(deviceInfo.roomEncryptionTrustLevel)
            }
        } else {
            views.sessionOverviewInfo.isVisible = false
        }
    }

    private fun updatePushNotificationToggle(deviceId: String, notificationsStatus: NotificationsStatus) {
        views.sessionOverviewPushNotifications.isGone = notificationsStatus == NotificationsStatus.NOT_SUPPORTED
        when (notificationsStatus) {
            NotificationsStatus.ENABLED, NotificationsStatus.DISABLED -> {
                views.sessionOverviewPushNotifications.setOnCheckedChangeListener(null)
                views.sessionOverviewPushNotifications.setChecked(notificationsStatus == NotificationsStatus.ENABLED)
                views.sessionOverviewPushNotifications.post {
                    views.sessionOverviewPushNotifications.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.handle(SessionOverviewAction.TogglePushNotifications(deviceId, isChecked))
                    }
                }
            }
            else -> Unit
        }
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading(null)
        } else {
            dismissLoadingDialog()
        }
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(SessionOverviewAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(SessionOverviewAction.PasswordAuthDone(password))
                }
                else -> {
                    viewModel.handle(SessionOverviewAction.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(SessionOverviewAction.ReAuthCancelled)
        }
    }

    /**
     * Launch the re auth activity to get credentials.
     */
    private fun askForReAuthentication(reAuthReq: SessionOverviewViewEvent.RequestReAuth) {
        ReAuthActivity.newIntent(
                requireContext(),
                reAuthReq.registrationFlowResponse,
                reAuthReq.lastErrorCode,
                getString(CommonStrings.devices_delete_dialog_title)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }

    private fun showLearnMoreInfoVerificationStatus(roomEncryptionTrustLevel: RoomEncryptionTrustLevel?) {
        val args = when (roomEncryptionTrustLevel) {
            null -> {
                // encryption not supported
                SessionLearnMoreBottomSheet.Args(
                        title = getString(CommonStrings.device_manager_verification_status_unverified),
                        description = getString(CommonStrings.device_manager_learn_more_sessions_encryption_not_supported),
                )
            }
            RoomEncryptionTrustLevel.Trusted -> {
                SessionLearnMoreBottomSheet.Args(
                        title = getString(CommonStrings.device_manager_verification_status_verified),
                        description = getString(CommonStrings.device_manager_learn_more_sessions_verified_description),
                )
            }
            else -> {
                SessionLearnMoreBottomSheet.Args(
                        title = getString(CommonStrings.device_manager_verification_status_unverified),
                        description = getString(CommonStrings.device_manager_learn_more_sessions_unverified),
                )
            }
        }
        SessionLearnMoreBottomSheet.show(childFragmentManager, args)
    }
}
