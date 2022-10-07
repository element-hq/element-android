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
        observeViewState()
    }

    private fun observeViewState() {
        viewModel.onEach {
            when (it.connectionStatus) {
                is QrCodeLoginConnectionStatus.Connected -> handleConnectionEstablished(it.connectionStatus)
                QrCodeLoginConnectionStatus.ConnectingToDevice -> handleConnectingToDevice()
                QrCodeLoginConnectionStatus.SigningIn -> handleSigningIn()
                null -> { /* NOOP */ }
            }
        }
    }

    private fun handleConnectingToDevice() {
        views.qrCodeLoginStatusLoadingLayout.isVisible = true
        views.qrCodeLoginStatusHeaderView.isVisible = false
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = true
        views.qrCodeLoginStatusLoadingTextView.setText(R.string.qr_code_login_connecting_to_device)
    }

    private fun handleSigningIn() {
        views.qrCodeLoginStatusLoadingLayout.isVisible = true
        views.qrCodeLoginStatusHeaderView.isVisible = false
        views.qrCodeLoginStatusSecurityCode.isVisible = false
        views.qrCodeLoginStatusNoMatchLayout.isVisible = false
        views.qrCodeLoginStatusCancelButton.isVisible = false
        views.qrCodeLoginStatusLoadingTextView.setText(R.string.qr_code_login_signing_in)
    }

    private fun handleConnectionEstablished(connectionStatus: QrCodeLoginConnectionStatus.Connected) {
        views.qrCodeLoginStatusLoadingLayout.isVisible = false
        views.qrCodeLoginStatusHeaderView.isVisible = true
        views.qrCodeLoginStatusSecurityCode.isVisible = true
        views.qrCodeLoginStatusNoMatchLayout.isVisible = true
        views.qrCodeLoginStatusCancelButton.isVisible = true
        views.qrCodeLoginStatusSecurityCode.text = connectionStatus.securityCode
        views.qrCodeLoginStatusHeaderView.setTitle(getString(R.string.qr_code_login_header_connected_title))
        views.qrCodeLoginStatusHeaderView.setDescription(getString(R.string.qr_code_login_header_connected_description))
        views.qrCodeLoginStatusHeaderView.setImage(
                imageResource = R.drawable.ic_qr_code_login_connected,
                backgroundTintColor = ThemeUtils.getColor(requireContext(), R.attr.colorPrimary)
        )
    }
}
