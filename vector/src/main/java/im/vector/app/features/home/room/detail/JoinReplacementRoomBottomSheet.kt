/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.parentFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetTombstoneJoinBinding
import javax.inject.Inject

@AndroidEntryPoint
class JoinReplacementRoomBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetTombstoneJoinBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetTombstoneJoinBinding.inflate(inflater, container, false)

    @Inject
    lateinit var errorFormatter: ErrorFormatter

    private val viewModel: TimelineViewModel by parentFragmentViewModel()

    override val showExpanded: Boolean
        get() = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.roomUpgradeButton.retryClicked = object : ClickListener {
            override fun invoke(view: View) {
                viewModel.handle(RoomDetailAction.JoinAndOpenReplacementRoom)
            }
        }

        viewModel.onEach(RoomDetailViewState::joinUpgradedRoomAsync) { joinState ->
            when (joinState) {
                // it should never be Uninitialized
                Uninitialized,
                is Loading    -> {
                    views.roomUpgradeButton.render(ButtonStateView.State.Loading)
                    views.descriptionText.setText(R.string.it_may_take_some_time)
                }
                is Success    -> {
                    views.roomUpgradeButton.render(ButtonStateView.State.Loaded)
                    dismiss()
                }
                is Fail       -> {
                    // display the error message
                    views.descriptionText.text = errorFormatter.toHumanReadable(joinState.error)
                    views.roomUpgradeButton.render(ButtonStateView.State.Error)
                }
            }
        }
    }
}
