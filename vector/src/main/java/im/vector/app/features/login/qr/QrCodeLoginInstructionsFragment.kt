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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentQrCodeLoginInstructionsBinding
import im.vector.app.features.qrcode.QrCodeScannerActivity
import im.vector.lib.strings.CommonStrings
import timber.log.Timber

@AndroidEntryPoint
class QrCodeLoginInstructionsFragment : VectorBaseFragment<FragmentQrCodeLoginInstructionsBinding>() {

    private val viewModel: QrCodeLoginViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeLoginInstructionsBinding {
        return FragmentQrCodeLoginInstructionsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initScanQrCodeButton()
        initShowQrCodeButton()
    }

    private fun initShowQrCodeButton() {
        views.qrCodeLoginInstructionsShowQrCodeButton.debouncedClicks {
            viewModel.handle(QrCodeLoginAction.ShowQrCode)
        }
    }

    private fun initScanQrCodeButton() {
        views.qrCodeLoginInstructionsScanQrCodeButton.debouncedClicks {
            QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
        }
    }

    private val scanActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val scannedQrCode = QrCodeScannerActivity.getResultText(activityResult.data)
            val wasQrCode = QrCodeScannerActivity.getResultIsQrCode(activityResult.data)

            Timber.d("Scanned QR code: $scannedQrCode, was QR code: $wasQrCode")
            if (wasQrCode && !scannedQrCode.isNullOrBlank()) {
                onQrCodeScanned(scannedQrCode)
            } else {
                onQrCodeScannerFailed()
            }
        }
    }

    private fun onQrCodeScanned(scannedQrCode: String) {
        viewModel.handle(QrCodeLoginAction.OnQrCodeScanned(scannedQrCode))
    }

    private fun onQrCodeScannerFailed() {
        // The user scanned something unexpected, so we try scanning again.
        // This seems to happen particularly with the large QRs needed for rendezvous
        // especially when the QR is partially off the screen
        Timber.d("QrCodeLoginInstructionsFragment.onQrCodeScannerFailed - showing scanner again")
        QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.loginType == QrCodeLoginType.LOGIN) {
            views.qrCodeLoginInstructionsView.setInstructions(
                    listOf(
                            getString(CommonStrings.qr_code_login_new_device_instruction_1),
                            getString(CommonStrings.qr_code_login_new_device_instruction_2),
                            getString(CommonStrings.qr_code_login_new_device_instruction_3),
                    )
            )
        } else {
            views.qrCodeLoginInstructionsView.setInstructions(
                    listOf(
                            getString(CommonStrings.qr_code_login_link_a_device_scan_qr_code_instruction_1),
                            getString(CommonStrings.qr_code_login_link_a_device_scan_qr_code_instruction_2),
                    )
            )
        }
    }
}
