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
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity

class QrCodeScannerActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_simple

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFirstCreation()) {
            replaceFragment(R.id.simpleFragmentContainer, QrCodeScannerFragment::class.java)
        }
    }

    fun setResultAndFinish(result: Result?) {
        if (result != null) {
            val rawBytes = getRawBytes(result)
            val rawBytesStr = rawBytes?.toString(Charsets.ISO_8859_1)

            setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_OUT_TEXT, rawBytesStr ?: result.text)
                putExtra(EXTRA_OUT_IS_QR_CODE, result.barcodeFormat == BarcodeFormat.QR_CODE)
            })
        }
        finish()
    }

    // Copied from https://github.com/markusfisch/BinaryEye/blob/
    // 9d57889b810dcaa1a91d7278fc45c262afba1284/app/src/main/kotlin/de/markusfisch/android/binaryeye/activity/CameraActivity.kt#L434
    private fun getRawBytes(result: Result): ByteArray? {
        val metadata = result.resultMetadata ?: return null
        val segments = metadata[ResultMetadataType.BYTE_SEGMENTS] ?: return null
        var bytes = ByteArray(0)
        @Suppress("UNCHECKED_CAST")
        for (seg in segments as Iterable<ByteArray>) {
            bytes += seg
        }
        // byte segments can never be shorter than the text.
        // Zxing cuts off content prefixes like "WIFI:"
        return if (bytes.size >= result.text.length) bytes else null
    }

    companion object {
        private const val EXTRA_OUT_TEXT = "EXTRA_OUT_TEXT"
        private const val EXTRA_OUT_IS_QR_CODE = "EXTRA_OUT_IS_QR_CODE"

        const val QR_CODE_SCANNER_REQUEST_CODE = 429

        // For test only
        fun startForResult(activity: Activity, requestCode: Int = QR_CODE_SCANNER_REQUEST_CODE) {
            activity.startActivityForResult(Intent(activity, QrCodeScannerActivity::class.java), requestCode)
        }

        fun startForResult(fragment: Fragment, requestCode: Int = QR_CODE_SCANNER_REQUEST_CODE) {
            fragment.startActivityForResult(Intent(fragment.requireActivity(), QrCodeScannerActivity::class.java), requestCode)
        }

        fun getResultText(data: Intent?): String? {
            return data?.getStringExtra(EXTRA_OUT_TEXT)
        }

        fun getResultIsQrCode(data: Intent?): Boolean {
            return data?.getBooleanExtra(EXTRA_OUT_IS_QR_CODE, false) == true
        }
    }
}
