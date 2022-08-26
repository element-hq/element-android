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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceListBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.home.HomeActivitySharedAction
import im.vector.app.features.home.HomeSharedActionViewModel
import im.vector.app.features.home.room.list.actions.RoomListSharedAction
import im.vector.app.features.home.room.list.actions.RoomListSharedActionViewModel
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

/**
 * This Fragment is displayed in the navigation drawer [im.vector.app.features.home.HomeDrawerFragment] and
 * is displaying the space hierarchy, with some actions on Spaces.
 *
 * In the New App Layout this fragment will instead be displayed in a Bottom Sheet [SpaceListBottomSheet]
 * and will only display spaces that are direct children of the currently selected space (or root spaces if none)
 */
@AndroidEntryPoint
class SpaceListFragment :
        VectorBaseFragment<FragmentSpaceListBinding>(),
        SpaceSummaryController.Callback,
        NewSpaceSummaryController.Callback {

    @Inject lateinit var spaceController: SpaceSummaryController
    @Inject lateinit var newSpaceController: NewSpaceSummaryController
    @Inject lateinit var vectorFeatures: VectorFeatures

    private lateinit var homeActivitySharedActionViewModel: HomeSharedActionViewModel
    private lateinit var roomListSharedActionViewModel: RoomListSharedActionViewModel
    private val viewModel: SpaceListViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSpaceListBinding {
        return FragmentSpaceListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeActivitySharedActionViewModel = activityViewModelProvider[HomeSharedActionViewModel::class.java]
        roomListSharedActionViewModel = activityViewModelProvider[RoomListSharedActionViewModel::class.java]
        views.stateView.contentView = views.groupListView
        setupSpaceController()
        observeViewEvents()
    }

    private fun setupSpaceController() {
        if (vectorFeatures.isNewAppLayoutEnabled()) {
            newSpaceController.callback = this
            views.groupListView.configureWith(newSpaceController)
        } else {
            enableDragAndDropForSpaceController()
            spaceController.callback = this
            views.groupListView.configureWith(spaceController)
        }
    }

    private fun enableDragAndDropForSpaceController() {
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
                        if (toPositionM == null || fromPositionM == null) return
                        val movingSpace = model?.matrixItem?.id ?: return
                        viewModel.handle(SpaceListAction.MoveSpace(movingSpace, toPositionM!! - fromPositionM!!))
                    }

                    override fun clearView(model: SpaceSummaryItem?, itemView: View?) {
                        itemView?.elevation = initialElevation ?: 0f
                    }

                    override fun onModelMoved(fromPosition: Int, toPosition: Int, modelBeingMoved: SpaceSummaryItem?, itemView: View?) {
                        if (fromPositionM == null) {
                            fromPositionM = fromPosition
                        }
                        toPositionM = toPosition
                        itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }

                    override fun isDragEnabledForModel(model: SpaceSummaryItem?): Boolean {
                        return model?.canDrag == true
                    }
                })
    }

    private fun observeViewEvents() = viewModel.observeViewEvents {
        when (it) {
            is SpaceListViewEvents.OpenSpaceSummary -> homeActivitySharedActionViewModel.post(HomeActivitySharedAction.OpenSpacePreview(it.id))
            is SpaceListViewEvents.AddSpace -> homeActivitySharedActionViewModel.post(HomeActivitySharedAction.AddSpace)
            is SpaceListViewEvents.OpenSpaceInvite -> homeActivitySharedActionViewModel.post(HomeActivitySharedAction.OpenSpaceInvite(it.id))
            SpaceListViewEvents.CloseDrawer -> homeActivitySharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
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
            is Loading -> {
                views.stateView.state = StateView.State.Loading
                return@withState
            }
            is Success -> views.stateView.state = StateView.State.Content
            else -> Unit
        }

        if (vectorFeatures.isNewAppLayoutEnabled()) {
            newSpaceController.update(state)
        } else {
            spaceController.update(state)
        }
    }

    override fun onSpaceSelected(spaceSummary: RoomSummary?) {
        viewModel.handle(SpaceListAction.SelectSpace(spaceSummary))
        roomListSharedActionViewModel.post(RoomListSharedAction.CloseBottomSheet)
    }

    override fun onSpaceInviteSelected(spaceSummary: RoomSummary) {
        viewModel.handle(SpaceListAction.OpenSpaceInvite(spaceSummary))
    }

    override fun onSpaceSettings(spaceSummary: RoomSummary) {
        homeActivitySharedActionViewModel.post(HomeActivitySharedAction.ShowSpaceSettings(spaceSummary.roomId))
    }

    override fun onToggleExpand(spaceSummary: RoomSummary) {
        viewModel.handle(SpaceListAction.ToggleExpand(spaceSummary))
    }

    override fun onAddSpaceSelected() {
        viewModel.handle(SpaceListAction.AddSpace)
    }

    override fun sendFeedBack() {
        homeActivitySharedActionViewModel.post(HomeActivitySharedAction.SendSpaceFeedBack)
    }
}
