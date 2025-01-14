/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetTombstoneJoinBinding
import im.vector.lib.strings.CommonStrings
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
                is Loading -> {
                    views.roomUpgradeButton.render(ButtonStateView.State.Loading)
                    views.descriptionText.setText(CommonStrings.it_may_take_some_time)
                }
                is Success -> {
                    views.roomUpgradeButton.render(ButtonStateView.State.Loaded)
                    dismiss()
                }
                is Fail -> {
                    // display the error message
                    views.descriptionText.text = errorFormatter.toHumanReadable(joinState.error)
                    views.roomUpgradeButton.render(ButtonStateView.State.Error)
                }
            }
        }
    }
}
