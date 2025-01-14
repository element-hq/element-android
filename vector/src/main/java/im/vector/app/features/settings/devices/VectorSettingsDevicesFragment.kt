/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.dialogs.ManuallyVerifyDialog
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

/**
 * Display the list of the user's device.
 */
@AndroidEntryPoint
class VectorSettingsDevicesFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        DevicesController.Callback {

    @Inject lateinit var devicesController: DevicesController

    // used to avoid requesting to enter the password for each deletion
    // Note: Sonar does not like to use password for member name.
//    private var mAccountPass: String = ""

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: DevicesViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.waitingView.waitingStatusText.setText(CommonStrings.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
        devicesController.callback = this
        views.genericRecyclerView.configureWith(devicesController, dividerDrawable = R.drawable.divider_horizontal)
        viewModel.observeViewEvents {
            when (it) {
                is DevicesViewEvents.Loading -> showLoading(it.message)
                is DevicesViewEvents.Failure -> showFailure(it.throwable)
                is DevicesViewEvents.RequestReAuth -> askForReAuthentication(it)
                is DevicesViewEvents.PromptRenameDevice -> displayDeviceRenameDialog(it.deviceInfo)
                is DevicesViewEvents.ShowVerifyDevice -> {
                    // TODO selfverif
//                    VerificationBottomSheet.withArgs(
// //                            roomId = null,
//                            otherUserId = it.userId,
//                            transactionId = it.transactionId ?: ""
//                    ).show(childFragmentManager, "REQPOP")
                }
                is DevicesViewEvents.SelfVerification -> {
                    navigator.requestSelfSessionVerification(requireActivity())
                }
                is DevicesViewEvents.ShowManuallyVerify -> {
                    ManuallyVerifyDialog.show(requireActivity(), it.cryptoDeviceInfo) {
                        viewModel.handle(DevicesAction.MarkAsManuallyVerified(it.cryptoDeviceInfo))
                    }
                }
                is DevicesViewEvents.PromptResetSecrets -> {
                    navigator.open4SSetup(requireActivity(), SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET)
                }
                is DevicesViewEvents.OpenBrowser -> {
                    openUrlInChromeCustomTab(requireContext(), null, it.url)
                }
            }
        }
    }

    override fun onDestroyView() {
        devicesController.callback = null
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.settings_active_sessions_manage)
        viewModel.handle(DevicesAction.Refresh)
    }

    override fun onDeviceClicked(deviceInfo: DeviceInfo) {
        DeviceVerificationInfoBottomSheet.newInstance(deviceInfo.userId ?: "", deviceInfo.deviceId ?: "").show(
                childFragmentManager,
                "VERIF_INFO"
        )
    }

    override fun retry() {
        viewModel.handle(DevicesAction.Refresh)
    }

    /**
     * Display an alert dialog to rename a device.
     *
     * @param deviceInfo device info
     */
    private fun displayDeviceRenameDialog(deviceInfo: DeviceInfo) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val views = DialogBaseEditTextBinding.bind(layout)
        views.editText.setText(deviceInfo.displayName)

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.devices_details_device_name)
                .setView(layout)
                .setPositiveButton(CommonStrings.ok) { _, _ ->
                    val newName = views.editText.text.toString()

                    viewModel.handle(DevicesAction.Rename(deviceInfo.deviceId!!, newName))
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
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
    private fun askForReAuthentication(reAuthReq: DevicesViewEvents.RequestReAuth) {
        ReAuthActivity.newIntent(
                requireContext(),
                reAuthReq.registrationFlowResponse,
                reAuthReq.lastErrorCode,
                getString(CommonStrings.devices_delete_dialog_title)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        devicesController.update(state)

        handleRequestStatus(state.request)
    }

    private fun handleRequestStatus(unIgnoreRequest: Async<Unit>) {
        views.waitingView.root.isVisible = when (unIgnoreRequest) {
            is Loading -> true
            else -> false
        }
    }
}
