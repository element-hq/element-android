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

import com.google.zxing.Result
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_qr_code_scanner.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import javax.inject.Inject

class QrCodeScannerFragment @Inject constructor()
    : VectorBaseFragment(),
        ZXingScannerView.ResultHandler {

    override fun getLayoutResId() = R.layout.fragment_qr_code_scanner

    override fun onResume() {
        super.onResume()
        // Register ourselves as a handler for scan results.
        scannerView.setResultHandler(this)
        // Start camera on resume
        scannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        // Stop camera on pause
        scannerView.stopCamera()
    }

    override fun handleResult(rawResult: Result?) {
        // Do something with the result here
        // This is not intended to be used outside of QrCodeScannerActivity for the moment
        (requireActivity() as? QrCodeScannerActivity)?.setResultAndFinish(rawResult)
    }
}
