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

package im.vector.app.features.login.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentQrCodeLoginStatusBinding
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason

@AndroidEntryPoint
class QrCodeLoginStatusFragment : VectorBaseFragment<FragmentQrCodeLoginStatusBinding>() {

    private val viewModel: QrCodeLoginViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeLoginStatusBinding {
        return FragmentQrCodeLoginStatusBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCancelButton()
        initTryAgainButton()
    }

    private fun initTryAgainButton() {
        views.qrCodeLoginStatusTryAgainButton.debouncedClicks {
            viewModel.handle(QrCodeLoginAction.TryAgain)
        }
    }

    private fun initCancelButton() {
        views.qrCodeLoginStatusCancelButton.debouncedClicks {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun handleFailed(connectionStatus: QrCodeLoginConnectionStatus.Failed) {
        views.qrCodeLoginConfirmSecurityCodeLayout.isVisible = false
        views.qrCodeLoginStatusLoadingLayout.isVisible = false
        views.qrCodeLoginStatusHeaderView.isVisible = true
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = true
        views.qrCodeLoginStatusTryAgainButton.isVisible = connectionStatus.canTryAgain
        views.qrCodeLoginStatusHeaderView.setTitle(getString(CommonStrings.qr_code_login_header_failed_title))
        views.qrCodeLoginStatusHeaderView.setDescription(getErrorDescription(connectionStatus.errorType))
        views.qrCodeLoginStatusHeaderView.setImage(
                imageResource = R.drawable.ic_qr_code_login_failed,
                backgroundTintColor = ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorError)
        )
    }

    private fun getErrorDescription(reason: RendezvousFailureReason): String {
        return when (reason) {
            RendezvousFailureReason.UnsupportedAlgorithm,
            RendezvousFailureReason.UnsupportedTransport -> getString(CommonStrings.qr_code_login_header_failed_device_is_not_supported_description)
            RendezvousFailureReason.UnsupportedHomeserver -> getString(CommonStrings.qr_code_login_header_failed_homeserver_is_not_supported_description)
            RendezvousFailureReason.Expired -> getString(CommonStrings.qr_code_login_header_failed_timeout_description)
            RendezvousFailureReason.UserDeclined -> getString(CommonStrings.qr_code_login_header_failed_denied_description)
            RendezvousFailureReason.E2EESecurityIssue -> getString(CommonStrings.qr_code_login_header_failed_e2ee_security_issue_description)
            RendezvousFailureReason.OtherDeviceAlreadySignedIn ->
                getString(CommonStrings.qr_code_login_header_failed_other_device_already_signed_in_description)
            RendezvousFailureReason.OtherDeviceNotSignedIn -> getString(CommonStrings.qr_code_login_header_failed_other_device_not_signed_in_description)
            RendezvousFailureReason.InvalidCode -> getString(CommonStrings.qr_code_login_header_failed_invalid_qr_code_description)
            RendezvousFailureReason.UserCancelled -> getString(CommonStrings.qr_code_login_header_failed_user_cancelled_description)
            else -> getString(CommonStrings.qr_code_login_header_failed_other_description)
        }
    }

    private fun handleConnectingToDevice() {
        views.qrCodeLoginConfirmSecurityCodeLayout.isVisible = false
        views.qrCodeLoginStatusLoadingLayout.isVisible = true
        views.qrCodeLoginStatusHeaderView.isVisible = false
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = true
        views.qrCodeLoginStatusTryAgainButton.isVisible = false
        views.qrCodeLoginStatusLoadingTextView.setText(CommonStrings.qr_code_login_connecting_to_device)
    }

    private fun handleSigningIn() {
        views.qrCodeLoginConfirmSecurityCodeLayout.isVisible = false
        views.qrCodeLoginStatusLoadingLayout.isVisible = true
        views.qrCodeLoginStatusHeaderView.apply {
            isVisible = true
            setTitle(getString(CommonStrings.dialog_title_success))
            setDescription("")
            setImage(R.drawable.ic_tick, ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
        }
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = false
        views.qrCodeLoginStatusTryAgainButton.isVisible = false
        views.qrCodeLoginStatusLoadingTextView.setText(CommonStrings.qr_code_login_signing_in)
    }

    private fun handleConnectionEstablished(connectionStatus: QrCodeLoginConnectionStatus.Connected, loginType: QrCodeLoginType) {
        views.qrCodeLoginConfirmSecurityCodeLayout.isVisible = loginType == QrCodeLoginType.LINK_A_DEVICE
        views.qrCodeLoginStatusLoadingLayout.isVisible = false
        views.qrCodeLoginStatusHeaderView.isVisible = true
        views.qrCodeLoginStatusSecurityCode.isVisible = true
        views.qrCodeLoginStatusNoMatchLayout.isVisible = loginType == QrCodeLoginType.LOGIN
        views.qrCodeLoginStatusCancelButton.isVisible = true
        views.qrCodeLoginStatusTryAgainButton.isVisible = false
        views.qrCodeLoginStatusSecurityCode.text = connectionStatus.securityCode
        views.qrCodeLoginStatusHeaderView.setTitle(getString(CommonStrings.qr_code_login_header_connected_title))
        views.qrCodeLoginStatusHeaderView.setDescription(getString(CommonStrings.qr_code_login_header_connected_description))
        views.qrCodeLoginStatusHeaderView.setImage(
                imageResource = R.drawable.ic_qr_code_login_connected,
                backgroundTintColor = ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary)
        )
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.connectionStatus) {
            is QrCodeLoginConnectionStatus.Connected -> handleConnectionEstablished(state.connectionStatus, state.loginType)
            QrCodeLoginConnectionStatus.ConnectingToDevice -> handleConnectingToDevice()
            QrCodeLoginConnectionStatus.SigningIn -> handleSigningIn()
            is QrCodeLoginConnectionStatus.Failed -> handleFailed(state.connectionStatus)
            null -> { /* NOOP */
            }
        }
    }
}
