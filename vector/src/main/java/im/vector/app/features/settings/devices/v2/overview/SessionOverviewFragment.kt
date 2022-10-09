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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentSessionOverviewBinding
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.list.SessionInfoViewState
import im.vector.app.features.settings.devices.v2.more.SessionLearnMoreBottomSheet
import im.vector.app.features.workers.signout.SignOutUiWorker
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.pushers.Pusher
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

    private fun confirmSignoutOtherSession() {
        activity?.let {
            MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.action_sign_out)
                    .setMessage(R.string.action_sign_out_confirmation_simple)
                    .setPositiveButton(R.string.action_sign_out) { _, _ ->
                        signoutSession()
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
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
        updateToolbar(state)
        updateSessionInfo(state)
        updateLoading(state.isLoading)
        updatePushNotificationToggle(state.deviceId, state.pushers.invoke().orEmpty())
        if (state.deviceInfo is Success) {
            renderSessionInfo(state.isCurrentSessionTrusted, state.deviceInfo.invoke())
        } else {
            hideSessionInfo()
        }
    }

    private fun updateToolbar(viewState: SessionOverviewViewState) {
        if (viewState.deviceInfo is Success) {
            val titleResId =
                    if (viewState.deviceInfo.invoke().isCurrentDevice) R.string.device_manager_current_session_title else R.string.device_manager_session_title
            (activity as? AppCompatActivity)
                    ?.supportActionBar
                    ?.setTitle(titleResId)
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
                    isLearnMoreLinkVisible = true,
                    isLastSeenDetailsVisible = true,
            )
            views.sessionOverviewInfo.render(infoViewState, dateFormatter, drawableProvider, colorProvider, stringProvider)
            views.sessionOverviewInfo.onLearnMoreClickListener = {
                showLearnMoreInfoVerificationStatus(deviceInfo.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Trusted)
            }
        } else {
            views.sessionOverviewInfo.isVisible = false
        }
    }

    private fun updatePushNotificationToggle(deviceId: String, pushers: List<Pusher>) {
        views.sessionOverviewPushNotifications.apply {
            if (pushers.isEmpty()) {
                isVisible = false
            } else {
                val allPushersAreEnabled = pushers.all { it.enabled }
                setOnCheckedChangeListener(null)
                setChecked(allPushersAreEnabled)
                post {
                    setOnCheckedChangeListener { _, isChecked ->
                        viewModel.handle(SessionOverviewAction.TogglePushNotifications(deviceId, isChecked))
                    }
                }
            }
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
                getString(R.string.devices_delete_dialog_title)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }

    private fun showLearnMoreInfoVerificationStatus(isVerified: Boolean) {
        val titleResId = if (isVerified) {
            R.string.device_manager_verification_status_verified
        } else {
            R.string.device_manager_verification_status_unverified
        }
        val descriptionResId = if (isVerified) {
            R.string.device_manager_learn_more_sessions_verified
        } else {
            R.string.device_manager_learn_more_sessions_unverified
        }
        val args = SessionLearnMoreBottomSheet.Args(
                title = getString(titleResId),
                description = getString(descriptionResId),
        )
        SessionLearnMoreBottomSheet.show(childFragmentManager, args)
    }

    private fun hideSessionInfo() {
        views.sessionOverviewInfo.isGone = true
    }
}
