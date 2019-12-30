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
package im.vector.riotx.features.crypto.verification

import android.text.style.ClickableSpan
import android.view.View
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.platform.parentFragmentViewModel
import im.vector.riotx.core.utils.tappableMatchingText
import kotlinx.android.synthetic.main.fragment_verification_choose_method.*
import javax.inject.Inject

class VerificationChooseMethodFragment @Inject constructor(
        val verificationChooseMethodViewModelFactory: VerificationChooseMethodViewModel.Factory
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_verification_choose_method

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    private val viewModel by fragmentViewModel(VerificationChooseMethodViewModel::class)

    override fun invalidate() = withState(viewModel) { state ->
        if (state.QRModeAvailable) {
            val cSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {

                }
            }
            val openLink = getString(R.string.verify_open_camera_link)
            val descCharSequence =
                    getString(R.string.verify_by_scanning_description, openLink)
                            .toSpannable()
                            .tappableMatchingText(openLink, cSpan)
            verifyQRDescription.text = descCharSequence
            verifyQRGroup.isVisible = true
        } else {
            verifyQRGroup.isVisible = false
        }

        verifyEmojiGroup.isVisible = state.SASMOdeAvailable
    }

    @OnClick(R.id.verificationByEmojiButton)
    fun doVerifyBySas() = withState(sharedViewModel) {
        sharedViewModel.handle(VerificationAction.StartSASVerification(it.otherUserMxItem?.id ?: "", it.pendingRequest?.transactionId
                ?: ""))
    }

}
