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

package im.vector.app.features.crypto.verification.user

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import im.vector.app.features.qrcode.QrCodeScannerActivity
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UserVerificationFragment : VectorBaseFragment<BottomSheetVerificationChildFragmentBinding>(),
        UserVerificationController.InteractionListener {

    @Inject lateinit var controller: UserVerificationController

    private val viewModel by parentFragmentViewModel(UserVerificationViewModel::class)

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
        Timber.w("VALR: invalidate with State: ${state.pendingRequest}")
        controller.update(state)
    }

    override fun acceptRequest() {
        viewModel.handle(VerificationAction.ReadyPendingVerification)
    }

    override fun declineRequest() {
        viewModel.handle(VerificationAction.CancelPendingVerification)
    }

    override fun onClickOnVerificationStart() {
        viewModel.handle(VerificationAction.RequestVerificationByDM)
    }

    override fun onDone(b: Boolean) {
//        viewModel.handle(VerificationAction.)
    }

    override fun onDoNotMatchButtonTapped() {
        viewModel.handle(VerificationAction.SASDoNotMatchAction)
    }

    override fun onMatchButtonTapped() {
        viewModel.handle(VerificationAction.SASMatchAction)
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

    private fun onRemoteQrCodeScanned(remoteQrCode: String) = withState(viewModel) { state ->
        viewModel.handle(
                VerificationAction.RemoteQrCodeScanned(
                        state.otherUserId,
                        state.pendingRequest.invoke()?.transactionId ?: "",
                        remoteQrCode
                )
        )
    }

    override fun doVerifyBySas() {
        viewModel.handle(VerificationAction.StartSASVerification)
    }

    override fun onUserDeniesQrCodeScanned() {
       viewModel.handle(VerificationAction.OtherUserDidNotScanned)
    }

    override fun onUserConfirmsQrCodeScanned() {
        viewModel.handle(VerificationAction.OtherUserScannedSuccessfully)
    }
}
