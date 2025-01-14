/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.toast
import im.vector.app.databinding.FragmentSpaceAddRoomsBinding
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import reactivecircus.flowbinding.appcompat.queryTextChanges
import javax.inject.Inject

@AndroidEntryPoint
class SpaceManageRoomsFragment :
        VectorBaseFragment<FragmentSpaceAddRoomsBinding>(),
        OnBackPressed,
        SpaceManageRoomsController.Listener,
        Callback {

    @Inject lateinit var epoxyController: SpaceManageRoomsController

    private val viewModel by fragmentViewModel(SpaceManageRoomsViewModel::class)
    private val sharedViewModel: SpaceManageSharedViewModel by activityViewModel()
    private val epoxyVisibilityTracker = EpoxyVisibilityTracker()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentSpaceAddRoomsBinding.inflate(inflater)

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        parentFragmentManager.popBackStack()
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.addRoomToSpaceToolbar)
                .setTitle(CommonStrings.space_manage_rooms_and_spaces)
                .allowBack()

        views.createNewRoom.isVisible = false
        epoxyController.listener = this
        views.roomList.configureWith(epoxyController, hasFixedSize = true, dividerDrawable = R.drawable.divider_horizontal)
        epoxyVisibilityTracker.attach(views.roomList)

        views.publicRoomsFilter.queryTextChanges()
                .debounce(200)
                .onEach {
                    viewModel.handle(SpaceManageRoomViewAction.UpdateFilter(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.onEach(SpaceManageRoomViewState::actionState) { actionState ->
            when (actionState) {
                is Loading -> {
                    sharedViewModel.handle(SpaceManagedSharedAction.ShowLoading)
                }
                else -> {
                    sharedViewModel.handle(SpaceManagedSharedAction.HideLoading)
                }
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                is SpaceManageRoomViewEvents.BulkActionFailure -> {
                    vectorBaseActivity.toast(errorFormatter.toHumanReadable(it.errorList.firstOrNull()))
                }
            }
        }
    }

    override fun onDestroyView() {
        epoxyController.listener = null
        epoxyVisibilityTracker.detach(views.roomList)
        views.roomList.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)

        state.spaceSummary.invoke()?.let {
            toolbar?.subtitle = it.displayName
        }

        if (state.selectedRooms.isNotEmpty()) {
            if (currentActionMode == null) {
                views.addRoomToSpaceToolbar.isVisible = true
                vectorBaseActivity.startSupportActionMode(this)
            } else {
                toolbar?.title = resources.getQuantityString(CommonPlurals.room_details_selected, state.selectedRooms.size, state.selectedRooms.size)
            }
//            views.addRoomToSpaceToolbar.isVisible = false
//            views.addRoomToSpaceToolbar.startActionMode(this)
        } else {
            currentActionMode?.finish()
        }
        Unit
    }

    var currentActionMode: ActionMode? = null

    override fun toggleSelection(childInfo: SpaceChildInfo) {
        viewModel.handle(SpaceManageRoomViewAction.ToggleSelection(childInfo.childRoomId))
    }

    override fun retry() {
        viewModel.handle(SpaceManageRoomViewAction.RefreshFromServer)
    }

    override fun loadAdditionalItemsIfNeeded() {
        viewModel.handle(SpaceManageRoomViewAction.LoadAdditionalItemsIfNeeded)
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.menu_manage_space, menu)
        withState(viewModel) {
            mode?.title = resources.getQuantityString(CommonPlurals.room_details_selected, it.selectedRooms.size, it.selectedRooms.size)
        }
        currentActionMode = mode
        views.addRoomToSpaceToolbar.isVisible = false
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        withState(viewModel) { state ->
            // check if we show mark as suggested or not
            val areAllSuggested = state.childrenInfo.invoke()?.children.orEmpty().filter { state.selectedRooms.contains(it.childRoomId) }
                    .all { it.suggested == true }
            menu?.findItem(R.id.action_mark_as_suggested)?.isVisible = !areAllSuggested
            menu?.findItem(R.id.action_mark_as_not_suggested)?.isVisible = areAllSuggested
        }

        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_delete -> {
                handleDeleteSelection()
            }
            R.id.action_mark_as_suggested -> {
                viewModel.handle(SpaceManageRoomViewAction.MarkAllAsSuggested(true))
            }
            R.id.action_mark_as_not_suggested -> {
                viewModel.handle(SpaceManageRoomViewAction.MarkAllAsSuggested(false))
            }
            else -> {
            }
        }
        mode?.finish()
        return true
    }

    private fun handleDeleteSelection() {
        viewModel.handle(SpaceManageRoomViewAction.BulkRemove)
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // should force a refresh
        currentActionMode = null
        viewModel.handle(SpaceManageRoomViewAction.ClearSelection)
        views.coordinatorLayout.post {
            if (isAdded) {
                TransitionManager.beginDelayedTransition(views.coordinatorLayout)
                views.addRoomToSpaceToolbar.isVisible = true
            }
        }
    }
}
