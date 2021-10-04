/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.usercode

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.airbnb.mvrx.activityViewModel
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentQrCodeScannerWithButtonBinding
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.utils.ImageUtils

import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class ScanUserCodeFragment @Inject constructor()
    : VectorBaseFragment<FragmentQrCodeScannerWithButtonBinding>(),
        ZXingScannerView.ResultHandler {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeScannerWithButtonBinding {
        return FragmentQrCodeScannerWithButtonBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: UserCodeSharedViewModel by activityViewModel()

    var autoFocus = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.userCodeMyCodeButton.debouncedClicks {
            sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SHOW))
        }

        views.userCodeOpenGalleryButton.debouncedClicks {
            MultiPicker.get(MultiPicker.IMAGE).single().startWith(pickImageActivityResultLauncher)
        }
    }

    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, _ ->
        if (allGranted) {
            startCamera()
        } else {
            // For now just go back
            sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SHOW))
        }
    }

    private val pickImageActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            MultiPicker
                    .get(MultiPicker.IMAGE)
                    .getSelectedFiles(requireActivity(), activityResult.data)
                    .firstOrNull()
                    ?.contentUri
                    ?.let { uri ->
                        // try to see if it is a valid matrix code
                        val bitmap = ImageUtils.getBitmap(requireContext(), uri)
                                ?: return@let Unit.also {
                                    Toast.makeText(requireContext(), getString(R.string.qr_code_not_scanned), Toast.LENGTH_SHORT).show()
                                }
                        handleResult(tryOrNull { QRCodeBitmapDecodeHelper.decodeQRFromBitmap(bitmap) })
                    }
        }
    }

    private fun startCamera() {
        views.userCodeScannerView.startCamera()
        views.userCodeScannerView.setAutoFocus(autoFocus)
        views.userCodeScannerView.debouncedClicks {
            this.autoFocus = !autoFocus
            views.userCodeScannerView.setAutoFocus(autoFocus)
        }
    }

    override fun onStart() {
        super.onStart()
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
            startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register ourselves as a handler for scan results.
        views.userCodeScannerView.setResultHandler(this)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        views.userCodeScannerView.setResultHandler(null)
        // Stop camera on pause
        views.userCodeScannerView.stopCamera()
    }

    override fun handleResult(result: Result?) {
        if (result === null) {
            Toast.makeText(requireContext(), R.string.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        } else {
            val rawBytes = getRawBytes(result)
            val rawBytesStr = rawBytes?.toString(Charsets.ISO_8859_1)
            val value = rawBytesStr ?: result.text
            sharedViewModel.handle(UserCodeActions.DecodedQRCode(value))
        }
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
}
