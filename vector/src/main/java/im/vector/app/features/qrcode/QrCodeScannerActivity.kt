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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding

@AndroidEntryPoint
class QrCodeScannerActivity() : VectorBaseActivity<ActivitySimpleBinding>() {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    private val qrViewModel: QrCodeScannerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        qrViewModel.observeViewEvents {
            when (it) {
                is QrCodeScannerEvents.CodeParsed  -> {
                    setResultAndFinish(it.result, it.isQrCode)
                }
                is QrCodeScannerEvents.ParseFailed -> {
                    Toast.makeText(this, R.string.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
                    finish()
                }
                else                               -> Unit
            }
        }

        if (isFirstCreation()) {
            val args = QrScannerArgs(showExtraButtons = false, R.string.verification_scan_their_code)
            replaceFragment(views.simpleFragmentContainer, QrCodeScannerFragment::class.java, args)
        }
    }

    private fun setResultAndFinish(result: String, isQrCode: Boolean) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_OUT_TEXT, result)
            putExtra(EXTRA_OUT_IS_QR_CODE, isQrCode)
        })
        finish()
    }

    companion object {
        private const val EXTRA_OUT_TEXT = "EXTRA_OUT_TEXT"
        private const val EXTRA_OUT_IS_QR_CODE = "EXTRA_OUT_IS_QR_CODE"

        fun startForResult(activity: Activity, activityResultLauncher: ActivityResultLauncher<Intent>) {
            activityResultLauncher.launch(Intent(activity, QrCodeScannerActivity::class.java))
        }

        fun getResultText(data: Intent?): String? {
            return data?.getStringExtra(EXTRA_OUT_TEXT)
        }

        fun getResultIsQrCode(data: Intent?): Boolean {
            return data?.getBooleanExtra(EXTRA_OUT_IS_QR_CODE, false) == true
        }
    }
}
