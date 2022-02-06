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

package im.vector.app.features.crypto.verification.cancel

import android.view.LayoutInflater
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseDialogFragment
import im.vector.app.databinding.DialogVerificationCancelBinding
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.home.AvatarRenderer
import javax.inject.Inject

@AndroidEntryPoint
class VerificationCancelDialogFragment : VectorBaseDialogFragment<DialogVerificationCancelBinding>() {

    companion object {
        fun newInstance(args : VerificationBottomSheet.VerificationArgs) : VerificationCancelDialogFragment {
            return VerificationCancelDialogFragment().apply {
                setArguments(args)
            }
        }
    }

    init {
        isCancelable = false
    }

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private val viewModel by fragmentViewModel(VerificationCancelViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): DialogVerificationCancelBinding {
        return DialogVerificationCancelBinding.inflate(inflater, container, false)
    }

    override fun invalidate() = withState(viewModel) { state ->
        state.userMxItem?.let { matrixItem ->

            avatarRenderer.render(matrixItem, views.otherUserAvatarImageView)
            views.otherUserShield.render(state.userTrustLevel)

            if (state.isMe) {
                if (state.currentDeviceCanCrossSign) {
                    views.dialogContent.text = getString(R.string.verify_cancel_self_verification_from_trusted)
                } else {
                    views.dialogContent.text = getString(R.string.verify_cancel_self_verification_from_untrusted)
                }
            } else {
                views.dialogContent.text = getString(R.string.verify_cancel_other, matrixItem.displayName, matrixItem.id)
            }

            views.btnContinueAction.setOnClickListener {
                VerificationBottomSheet.withArgs(
                        roomId = state.roomId,
                        otherUserId = state.otherUserId,
                        transactionId = state.transactionId
                ).show(parentFragmentManager, "REQ")
                dismiss()
            }

            views.btnSkipAction.setOnClickListener {
                viewModel.confirmCancel()
                dismiss()
            }
        }

        return@withState
    }
}
