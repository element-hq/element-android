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

import android.os.Parcelable
import androidx.core.content.ContextCompat
import butterknife.OnClick
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.VectorBaseFragment
import io.noties.markwon.Markwon
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_verification_conclusion.*
import javax.inject.Inject

class VerificationConclusionFragment @Inject constructor() : VectorBaseFragment() {

    @Parcelize
    data class Args(
            val isSuccessFull: Boolean,
            val cancelReason: String?
    ) : Parcelable

    override fun getLayoutResId() = R.layout.fragment_verification_conclusion

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    private val viewModel by fragmentViewModel(VerificationConclusionViewModel::class)

    override fun invalidate() = withState(viewModel) {
        when (it.conclusionState) {
            ConclusionState.SUCCESS   -> {
                verificationConclusionTitle.text = getString(R.string.sas_verified)
                verifyConclusionDescription.setTextOrHide(getString(R.string.sas_verified_successful_description))
                verifyConclusionBottomDescription.text = getString(R.string.verification_green_shield)
                verifyConclusionImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_shield_trusted))
            }
            ConclusionState.WARNING   -> {
                verificationConclusionTitle.text = getString(R.string.verification_conclusion_not_secure)
                verifyConclusionDescription.setTextOrHide(null)
                verifyConclusionImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_shield_warning))

                verifyConclusionBottomDescription.text = Markwon.builder(requireContext())
                        .build()
                        .toMarkdown(getString(R.string.verification_conclusion_compromised))
            }
            ConclusionState.CANCELLED -> {
                // Just dismiss in this case
                sharedViewModel.handle(VerificationAction.GotItConclusion)
            }
        }
    }

    @OnClick(R.id.verificationConclusionButton)
    fun onButtonTapped() {
        sharedViewModel.handle(VerificationAction.GotItConclusion)
    }
}
