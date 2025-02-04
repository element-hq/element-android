/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.qrcode

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentQrCodeScannerBinding
import im.vector.app.features.usercode.QRCodeBitmapDecodeHelper
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.utils.ImageUtils
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.matrix.android.sdk.api.extensions.tryOrNull

@Parcelize
data class QrScannerArgs(
        val showExtraButtons: Boolean,
        @StringRes val titleRes: Int
) : Parcelable

@AndroidEntryPoint
class QrCodeScannerFragment :
        VectorBaseFragment<FragmentQrCodeScannerBinding>(),
        ZXingScannerView.ResultHandler {

    private val qrViewModel: QrCodeScannerViewModel by activityViewModel()
    private val scannerArgs: QrScannerArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentQrCodeScannerBinding {
        return FragmentQrCodeScannerBinding.inflate(inflater, container, false)
    }

    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            startCamera()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(CommonStrings.denied_permission_camera)
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
                                    Toast.makeText(requireContext(), getString(CommonStrings.qr_code_not_scanned), Toast.LENGTH_SHORT).show()
                                }
                        handleResult(tryOrNull { QRCodeBitmapDecodeHelper.decodeQRFromBitmap(bitmap) })
                    }
        }
    }

    private var autoFocus = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = scannerArgs.titleRes.let { getString(it) }

        setupToolbar(views.qrScannerToolbar)
                .setTitle(title)
                .allowBack(useCross = true)

        scannerArgs.showExtraButtons.let { showButtons ->
            views.userCodeMyCodeButton.isVisible = showButtons
            views.userCodeOpenGalleryButton.isVisible = showButtons

            if (showButtons) {
                views.userCodeOpenGalleryButton.debouncedClicks {
                    MultiPicker.get(MultiPicker.IMAGE).single().startWith(pickImageActivityResultLauncher)
                }
                views.userCodeMyCodeButton.debouncedClicks {
                    qrViewModel.handle(QrCodeScannerAction.SwitchMode)
                }
            }
        }
    }

    private fun startCamera() {
        with(views.qrScannerView) {
            startCamera()
            setAutoFocus(autoFocus)
            debouncedClicks {
                autoFocus = !autoFocus
                setAutoFocus(autoFocus)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.hideKeyboard()

        // Register ourselves as a handler for scan results.
        views.qrScannerView.setResultHandler(this)

        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        views.qrScannerView.setResultHandler(null)
        views.qrScannerView.stopCamera()
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

    override fun handleResult(rawResult: Result?) {
        if (rawResult == null) {
            qrViewModel.handle(QrCodeScannerAction.ScanFailed)
        } else {
            val rawBytes = getRawBytes(rawResult)
            val rawBytesStr = rawBytes?.toString(Charsets.ISO_8859_1)
            val result = rawBytesStr ?: rawResult.text
            val isQrCode = rawResult.barcodeFormat == BarcodeFormat.QR_CODE
            qrViewModel.handle(QrCodeScannerAction.CodeDecoded(result, isQrCode))
        }
    }
}
