/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.qrcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.Result
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentQrCodeScannerBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import javax.inject.Inject

class QrCodeScannerFragment @Inject constructor() :
    VectorBaseFragment<FragmentQrCodeScannerBinding>(),
        ZXingScannerView.ResultHandler {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeScannerBinding {
        return FragmentQrCodeScannerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.qrScannerClose.debouncedClicks {
            requireActivity().onBackPressed()
        }
        views.qrScannerTitle.text = getString(R.string.verification_scan_their_code)
    }

    override fun onResume() {
        super.onResume()
        // Register ourselves as a handler for scan results.
        views.scannerView.setResultHandler(this)
        // Start camera on resume
        views.scannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        // Stop camera on pause
        views.scannerView.stopCamera()
    }

    override fun handleResult(rawResult: Result?) {
        // Do something with the result here
        // This is not intended to be used outside of QrCodeScannerActivity for the moment
        (requireActivity() as? QrCodeScannerActivity)?.setResultAndFinish(rawResult)
    }
}
