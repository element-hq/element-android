/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.verification.choose

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.BottomSheetVerificationChildFragmentBinding
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewModel
import im.vector.app.features.qrcode.QrCodeScannerActivity
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VerificationChooseMethodFragment :
        VectorBaseFragment<BottomSheetVerificationChildFragmentBinding>(),
        VerificationChooseMethodController.Listener {

    @Inject lateinit var controller: VerificationChooseMethodController
    private val viewModel by fragmentViewModel(VerificationChooseMethodViewModel::class)

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetVerificationChildFragmentBinding {
        return BottomSheetVerificationChildFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    override fun onDestroyView() {
        views.bottomSheetVerificationRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.bottomSheetVerificationRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.listener = this
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.update(state)
    }

    override fun doVerifyBySas() = withState(sharedViewModel) { state ->
        sharedViewModel.handle(
                VerificationAction.StartSASVerification(
                        state.otherUserId,
                        state.pendingRequest.invoke()?.transactionId ?: ""
                )
        )
    }

    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOpenQRCodeScanner()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(R.string.denied_permission_camera)
        }
    }

    override fun openCamera() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
            doOpenQRCodeScanner()
        }
    }

    override fun onClickOnWasNotMe() {
        sharedViewModel.itWasNotMe()
    }

    override fun acceptRequest() {
        sharedViewModel.handle(VerificationAction.ReadyPendingVerification)
    }

    override fun declineRequest() {
        sharedViewModel.handle(VerificationAction.CancelPendingVerification)
    }

    private fun doOpenQRCodeScanner() {
        QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
    }

    private val scanActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val scannedQrCode = QrCodeScannerActivity.getResultText(activityResult.data)
            val wasQrCode = QrCodeScannerActivity.getResultIsQrCode(activityResult.data)

            if (wasQrCode && !scannedQrCode.isNullOrBlank()) {
                onRemoteQrCodeScanned(scannedQrCode)
            } else {
                Timber.w("It was not a QR code, or empty result")
            }
        }
    }

    private fun onRemoteQrCodeScanned(remoteQrCode: String) = withState(sharedViewModel) { state ->
        sharedViewModel.handle(
                VerificationAction.RemoteQrCodeScanned(
                        state.otherUserId,
                        state.pendingRequest.invoke()?.transactionId ?: "",
                        remoteQrCode
                )
        )
    }
}
