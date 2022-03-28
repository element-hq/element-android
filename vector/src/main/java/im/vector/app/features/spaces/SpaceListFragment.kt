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

package im.vector.app.features.spaces

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyTouchHelper
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGroupListBinding
import im.vector.app.features.home.HomeActivitySharedAction
import im.vector.app.features.home.HomeSharedActionViewModel
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class SpaceListFragment @Inject constructor(
        private val spaceController: SpaceSummaryController
) : VectorBaseFragment<FragmentGroupListBinding>(), SpaceSummaryController.Callback {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private val viewModel: SpaceListViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGroupListBinding {
        return FragmentGroupListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)
        spaceController.callback = this
        views.stateView.contentView = views.groupListView
        views.groupListView.configureWith(spaceController)
        EpoxyTouchHelper.initDragging(spaceController)
                .withRecyclerView(views.groupListView)
                .forVerticalList()
                .withTarget(SpaceSummaryItem::class.java)
                .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<SpaceSummaryItem>() {
                    var toPositionM: Int? = null
                    var fromPositionM: Int? = null
                    var initialElevation: Float? = null

                    override fun onDragStarted(model: SpaceSummaryItem?, itemView: View?, adapterPosition: Int) {
                        toPositionM = null
                        fromPositionM = null
                        model?.matrixItem?.id?.let {
                            viewModel.handle(SpaceListAction.OnStartDragging(it, model.expanded))
                        }
                        itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        initialElevation = itemView?.elevation
                        itemView?.elevation = 6f
                    }

                    override fun onDragReleased(model: SpaceSummaryItem?, itemView: View?) {
//                        Timber.v("VAL: onModelMoved from $fromPositionM to $toPositionM ${model?.matrixItem?.getBestName()}")
                        if (toPositionM == null || fromPositionM == null) return
                        val movingSpace = model?.matrixItem?.id ?: return
                        viewModel.handle(SpaceListAction.MoveSpace(movingSpace, toPositionM!! - fromPositionM!!))
                    }

                    override fun clearView(model: SpaceSummaryItem?, itemView: View?) {
//                        Timber.v("VAL: clearView ${model?.matrixItem?.getBestName()}")
                        itemView?.elevation = initialElevation ?: 0f
                    }

                    override fun onModelMoved(fromPosition: Int, toPosition: Int, modelBeingMoved: SpaceSummaryItem?, itemView: View?) {
//                        Timber.v("VAL: onModelMoved incremental from $fromPosition to $toPosition ${modelBeingMoved?.matrixItem?.getBestName()}")
                        if (fromPositionM == null) {
                            fromPositionM = fromPosition
                        }
                        toPositionM = toPosition
                        itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }

                    override fun isDragEnabledForModel(model: SpaceSummaryItem?): Boolean {
//                        Timber.v("VAL: isDragEnabledForModel ${model?.matrixItem?.getBestName()}")
                        return model?.canDrag == true
                    }
                })

        viewModel.observeViewEvents {
            when (it) {
                is SpaceListViewEvents.OpenSpaceSummary -> sharedActionViewModel.post(HomeActivitySharedAction.OpenSpacePreview(it.id))
                is SpaceListViewEvents.OpenSpace        -> sharedActionViewModel.post(HomeActivitySharedAction.OpenGroup(it.groupingMethodHasChanged))
                is SpaceListViewEvents.AddSpace         -> sharedActionViewModel.post(HomeActivitySharedAction.AddSpace)
                is SpaceListViewEvents.OpenGroup        -> sharedActionViewModel.post(HomeActivitySharedAction.OpenGroup(it.groupingMethodHasChanged))
                is SpaceListViewEvents.OpenSpaceInvite  -> sharedActionViewModel.post(HomeActivitySharedAction.OpenSpaceInvite(it.id))
            }
        }
    }

    override fun onDestroyView() {
        spaceController.callback = null
        views.groupListView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.asyncSpaces) {
            Uninitialized,
            is Loading -> views.stateView.state = StateView.State.Loading
            is Success -> views.stateView.state = StateView.State.Content
            else       -> Unit
        }
        spaceController.update(state)
    }

    override fun onSpaceSelected(spaceSummary: RoomSummary?) {
        viewModel.handle(SpaceListAction.SelectSpace(spaceSummary))
    }

    override fun onSpaceInviteSelected(spaceSummary: RoomSummary) {
        viewModel.handle(SpaceListAction.OpenSpaceInvite(spaceSummary))
    }

    override fun onSpaceSettings(spaceSummary: RoomSummary) {
        sharedActionViewModel.post(HomeActivitySharedAction.ShowSpaceSettings(spaceSummary.roomId))
    }

    override fun onToggleExpand(spaceSummary: RoomSummary) {
        viewModel.handle(SpaceListAction.ToggleExpand(spaceSummary))
    }

    override fun onAddSpaceSelected() {
        viewModel.handle(SpaceListAction.AddSpace)
    }

    override fun onGroupSelected(groupSummary: GroupSummary?) {
        viewModel.handle(SpaceListAction.SelectLegacyGroup(groupSummary))
    }

    override fun sendFeedBack() {
        sharedActionViewModel.post(HomeActivitySharedAction.SendSpaceFeedBack)
    }
}
