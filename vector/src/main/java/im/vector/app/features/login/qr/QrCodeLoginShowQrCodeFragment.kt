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
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentQrCodeLoginShowQrCodeBinding
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class QrCodeLoginShowQrCodeFragment : VectorBaseFragment<FragmentQrCodeLoginShowQrCodeBinding>() {

    private val viewModel: QrCodeLoginViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeLoginShowQrCodeBinding {
        return FragmentQrCodeLoginShowQrCodeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCancelButton()
        viewModel.handle(QrCodeLoginAction.GenerateQrCode)
    }

    private fun initCancelButton() {
        views.qrCodeLoginShowQrCodeCancelButton.debouncedClicks {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setInstructions(loginType: QrCodeLoginType) {
        if (loginType == QrCodeLoginType.LOGIN) {
            views.qrCodeLoginShowQrCodeHeaderView.setDescription(getString(CommonStrings.qr_code_login_header_show_qr_code_new_device_description))
            views.qrCodeLoginShowQrCodeInstructionsView.setInstructions(
                    listOf(
                            getString(CommonStrings.qr_code_login_new_device_instruction_1),
                            getString(CommonStrings.qr_code_login_new_device_instruction_2),
                            getString(CommonStrings.qr_code_login_new_device_instruction_3),
                    )
            )
        } else {
            views.qrCodeLoginShowQrCodeHeaderView.setDescription(getString(CommonStrings.qr_code_login_header_show_qr_code_link_a_device_description))
            views.qrCodeLoginShowQrCodeInstructionsView.setInstructions(
                    listOf(
                            getString(CommonStrings.qr_code_login_link_a_device_show_qr_code_instruction_1),
                            getString(CommonStrings.qr_code_login_link_a_device_show_qr_code_instruction_2),
                    )
            )
        }
    }

    private fun showQrCode(qrCodeData: String) {
        views.qrCodeLoginSHowQrCodeImageView.setData(qrCodeData)
    }

    override fun invalidate() = withState(viewModel) { state ->
        state.generatedQrCodeData?.let { qrCodeData ->
            showQrCode(qrCodeData)
        }
        setInstructions(state.loginType)
    }
}
