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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentQrCodeLoginShowQrCodeBinding

@AndroidEntryPoint
class QrCodeLoginShowQrCodeFragment : VectorBaseFragment<FragmentQrCodeLoginShowQrCodeBinding>() {

    private val viewModel: QrCodeLoginViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeLoginShowQrCodeBinding {
        return FragmentQrCodeLoginShowQrCodeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCancelButton()
        observeViewState()
        viewModel.handle(QrCodeLoginAction.QrCodeViewStarted)
    }

    private fun initCancelButton() {
        views.qrCodeLoginShowQrCodeCancelButton.debouncedClicks {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeViewState() {
        viewModel.onEach {
            it.generatedQrCodeData?.let { qrCodeData ->
                showQrCode(qrCodeData)
            }
        }
    }

    private fun showQrCode(qrCodeData: String) {
        views.qrCodeLoginSHowQrCodeImageView.setData(qrCodeData)
    }
}
