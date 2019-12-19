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
import android.os.Bundle
import androidx.core.text.toSpannable
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import butterknife.OnClick
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.extensions.commitTransaction
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.platform.parentFragmentViewModel
import im.vector.riotx.core.utils.colorizeMatchingText
import im.vector.riotx.core.utils.styleMatchingText
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.themes.ThemeUtils
import kotlinx.android.synthetic.main.fragment_verification_request.*
import javax.inject.Inject

class OutgoingVerificationRequestFragment @Inject constructor(
        val outgoingVerificationRequestViewModelFactory: OutgoingVerificationRequestViewModel.Factory,
        val avatarRenderer: AvatarRenderer
) : VectorBaseFragment() {

    private val viewModel by fragmentViewModel(OutgoingVerificationRequestViewModel::class)

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getLayoutResId() = R.layout.fragment_verification_request

    override fun invalidate() = withState(viewModel) { state ->
        state.matrixItem?.let {
            val styledText = getString(R.string.verification_request_alert_description, it.id)
                    .toSpannable()
                    .styleMatchingText(it.id, Typeface.BOLD)
                    .colorizeMatchingText(it.id, ThemeUtils.getColor(requireContext(), R.attr.vctr_notice_text_color))
            verificationRequestText.text = styledText
        }
        Unit
    }

    @OnClick(R.id.verificationStartButton)
    fun onClickOnVerificationStart() = withState(viewModel) { state ->

        sharedViewModel.handle(VerificationAction.RequestVerificationByDM(state.otherUserId))

        getParentCoordinatorLayout()?.let {
            TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 150 })
        }
        parentFragmentManager.commitTransaction {
            replace(R.id.bottomSheetFragmentContainer,
                    VerificationChooseMethodFragment::class.java,
                    Bundle().apply { putString(MvRx.KEY_ARG, state.otherUserId) },
                    "REQUEST"
            )
        }
    }

}
