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

import android.graphics.Typeface
import androidx.core.text.toSpannable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import butterknife.OnClick
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.colorizeMatchingText
import im.vector.riotx.core.utils.styleMatchingText
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.themes.ThemeUtils
import kotlinx.android.synthetic.main.fragment_verification_request.*
import javax.inject.Inject

class VerificationRequestFragment @Inject constructor(
        val verificationRequestViewModelFactory: VerificationRequestViewModel.Factory,
        val avatarRenderer: AvatarRenderer
) : VectorBaseFragment() {

    private val viewModel by fragmentViewModel(VerificationRequestViewModel::class)

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getLayoutResId() = R.layout.fragment_verification_request

    override fun invalidate() = withState(viewModel) { state ->
        state.matrixItem.let {
            val styledText = getString(R.string.verification_request_alert_description, it.id)
                    .toSpannable()
                    .styleMatchingText(it.id, Typeface.BOLD)
                    .colorizeMatchingText(it.id, ThemeUtils.getColor(requireContext(), R.attr.vctr_notice_text_color))
            verificationRequestText.text = styledText
        }

        when (state.started) {
            is Loading -> {
                // Hide the start button, show waiting
                verificationStartButton.isInvisible = true
                verificationWaitingText.isVisible = true
                val otherUser = state.matrixItem.displayName ?: state.matrixItem.id
                verificationWaitingText.text = getString(R.string.verification_request_waiting_for, otherUser)
                        .toSpannable()
                        .styleMatchingText(otherUser, Typeface.BOLD)
                        .colorizeMatchingText(otherUser, ThemeUtils.getColor(requireContext(), R.attr.vctr_notice_text_color))
            }
            else       -> {
                verificationStartButton.isEnabled = true
                verificationStartButton.isVisible = true
                verificationWaitingText.isInvisible = true
            }
        }

        Unit
    }

    @OnClick(R.id.verificationStartButton)
    fun onClickOnVerificationStart() = withState(viewModel) { state ->
        verificationStartButton.isEnabled = false
        sharedViewModel.handle(VerificationAction.RequestVerificationByDM(state.matrixItem.id, state.roomId))
    }
}
