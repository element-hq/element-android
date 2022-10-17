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

@AndroidEntryPoint
class QrCodeLoginStatusFragment : VectorBaseFragment<FragmentQrCodeLoginStatusBinding>() {

    private val viewModel: QrCodeLoginViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeLoginStatusBinding {
        return FragmentQrCodeLoginStatusBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCancelButton()
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
        views.qrCodeLoginStatusHeaderView.setTitle(getString(R.string.qr_code_login_header_failed_title))
        views.qrCodeLoginStatusHeaderView.setDescription(getErrorCode(connectionStatus.errorType))
        views.qrCodeLoginStatusHeaderView.setImage(
                imageResource = R.drawable.ic_qr_code_login_failed,
                backgroundTintColor = ThemeUtils.getColor(requireContext(), R.attr.colorError)
        )
    }

    private fun getErrorCode(errorType: QrCodeLoginErrorType): String {
        return when (errorType) {
            QrCodeLoginErrorType.DEVICE_IS_NOT_SUPPORTED -> getString(R.string.qr_code_login_header_failed_device_is_not_supported_description)
            QrCodeLoginErrorType.TIMEOUT -> getString(R.string.qr_code_login_header_failed_timeout_description)
            QrCodeLoginErrorType.REQUEST_WAS_DENIED -> getString(R.string.qr_code_login_header_failed_denied_description)
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
        views.qrCodeLoginStatusLoadingTextView.setText(R.string.qr_code_login_connecting_to_device)
    }

    private fun handleSigningIn() {
        views.qrCodeLoginConfirmSecurityCodeLayout.isVisible = false
        views.qrCodeLoginStatusLoadingLayout.isVisible = true
        views.qrCodeLoginStatusHeaderView.apply {
            isVisible = true
            setTitle(getString(R.string.dialog_title_success))
            setDescription("")
            setImage(R.drawable.ic_tick, ThemeUtils.getColor(requireContext(), R.attr.colorPrimary))
        }
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = false
        views.qrCodeLoginStatusTryAgainButton.isVisible = false
        views.qrCodeLoginStatusLoadingTextView.setText(R.string.qr_code_login_signing_in)
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
        views.qrCodeLoginStatusHeaderView.setTitle(getString(R.string.qr_code_login_header_connected_title))
        views.qrCodeLoginStatusHeaderView.setDescription(getString(R.string.qr_code_login_header_connected_description))
        views.qrCodeLoginStatusHeaderView.setImage(
                imageResource = R.drawable.ic_qr_code_login_connected,
                backgroundTintColor = ThemeUtils.getColor(requireContext(), R.attr.colorPrimary)
        )
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.connectionStatus) {
            is QrCodeLoginConnectionStatus.Connected -> handleConnectionEstablished(state.connectionStatus, state.loginType)
            QrCodeLoginConnectionStatus.ConnectingToDevice -> handleConnectingToDevice()
            QrCodeLoginConnectionStatus.SigningIn -> handleSigningIn()
            is QrCodeLoginConnectionStatus.Failed -> handleFailed(state.connectionStatus)
            null -> { /* NOOP */ }
        }
    }
}
