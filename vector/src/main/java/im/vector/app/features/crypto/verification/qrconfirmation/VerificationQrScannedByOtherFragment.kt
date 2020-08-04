/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.features.crypto.verification.qrconfirmation

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewModel
import kotlinx.android.synthetic.main.bottom_sheet_verification_child_fragment.*
import javax.inject.Inject

class VerificationQrScannedByOtherFragment @Inject constructor(
        val controller: VerificationQrScannedByOtherController
) : VectorBaseFragment(), VerificationQrScannedByOtherController.Listener {

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getLayoutResId() = R.layout.bottom_sheet_verification_child_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        controller.update(state)
    }

    override fun onDestroyView() {
        bottomSheetVerificationRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        bottomSheetVerificationRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.listener = this
    }

    override fun onUserConfirmsQrCodeScanned() {
        sharedViewModel.handle(VerificationAction.OtherUserScannedSuccessfully)
    }

    override fun onUserDeniesQrCodeScanned() {
        sharedViewModel.handle(VerificationAction.OtherUserDidNotScanned)
    }
}
