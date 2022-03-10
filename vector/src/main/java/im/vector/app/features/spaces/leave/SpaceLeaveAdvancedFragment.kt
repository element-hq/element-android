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

package im.vector.app.features.spaces.leave

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceLeaveAdvancedBinding
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import reactivecircus.flowbinding.appcompat.queryTextChanges
import javax.inject.Inject

class SpaceLeaveAdvancedFragment @Inject constructor(
        val controller: SelectChildrenController
) : VectorBaseFragment<FragmentSpaceLeaveAdvancedBinding>(),
        SelectChildrenController.Listener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceLeaveAdvancedBinding.inflate(layoutInflater, container, false)

    val viewModel: SpaceLeaveAdvancedViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.toolbar)
                .allowBack()
        controller.listener = this
        views.roomList.configureWith(controller)
        views.spaceLeaveCancel.debouncedClicks { requireActivity().finish() }

        views.spaceLeaveButton.debouncedClicks {
            viewModel.handle(SpaceLeaveAdvanceViewAction.DoLeave)
        }

        views.publicRoomsFilter.queryTextChanges()
                .debounce(100)
                .onEach {
                    viewModel.handle(SpaceLeaveAdvanceViewAction.UpdateFilter(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        controller.listener = null
        views.roomList.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        controller.setData(state)
    }

    override fun onItemSelected(roomSummary: RoomSummary) {
        viewModel.handle(SpaceLeaveAdvanceViewAction.ToggleSelection(roomSummary.roomId))
    }
}
