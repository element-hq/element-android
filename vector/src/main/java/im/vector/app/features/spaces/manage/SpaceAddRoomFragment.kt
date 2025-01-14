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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.databinding.FragmentSpaceAddRoomsBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import reactivecircus.flowbinding.appcompat.queryTextChanges
import javax.inject.Inject

@AndroidEntryPoint
class SpaceAddRoomFragment :
        VectorBaseFragment<FragmentSpaceAddRoomsBinding>(),
        OnBackPressed,
        AddRoomListController.Listener,
        VectorMenuProvider {

    @Inject lateinit var spaceEpoxyController: AddRoomListController
    @Inject lateinit var roomEpoxyController: AddRoomListController
    @Inject lateinit var dmEpoxyController: AddRoomListController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceAddRoomsBinding.inflate(layoutInflater, container, false)

    private val viewModel by fragmentViewModel(SpaceAddRoomsViewModel::class)

    private val sharedViewModel: SpaceManageSharedViewModel by activityViewModel()

    override fun getMenuRes(): Int = R.menu.menu_space_add_room

    private var saveNeeded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.addRoomToSpaceToolbar)
                .allowBack()

//        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()

        views.publicRoomsFilter.queryTextChanges()
                .debounce(100)
                .onEach {
                    viewModel.handle(SpaceAddRoomActions.UpdateFilter(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        spaceEpoxyController.subHeaderText = getString(CommonStrings.spaces_feeling_experimental_subspace)
        viewModel.selectionListLiveData.observe(viewLifecycleOwner) {
            spaceEpoxyController.selectedItems = it
            roomEpoxyController.selectedItems = it
            dmEpoxyController.selectedItems = it
            saveNeeded = it.values.any { it }
            invalidateOptionsMenu()
        }

        viewModel.onEach(SpaceAddRoomsState::spaceName) {
            toolbar?.subtitle = it
        }

        viewModel.onEach(SpaceAddRoomsState::ignoreRooms) {
            spaceEpoxyController.ignoreRooms = it
            roomEpoxyController.ignoreRooms = it
            dmEpoxyController.ignoreRooms = it
        }

        viewModel.onEach(SpaceAddRoomsState::isSaving) {
            if (it is Loading) {
                sharedViewModel.handle(SpaceManagedSharedAction.ShowLoading)
            } else {
                sharedViewModel.handle(SpaceManagedSharedAction.HideLoading)
            }
        }

        viewModel.onEach(SpaceAddRoomsState::shouldShowDMs) {
            dmEpoxyController.disabled = !it
        }

        viewModel.onEach(SpaceAddRoomsState::onlyShowSpaces) {
            spaceEpoxyController.disabled = !it
            roomEpoxyController.disabled = it
            views.createNewRoom.text = if (it) getString(CommonStrings.create_space) else getString(CommonStrings.create_new_room)
            toolbar?.setTitle(if (it) CommonStrings.space_add_existing_spaces else CommonStrings.space_add_existing_rooms_only)
        }

        views.createNewRoom.debouncedClicks {
            withState(viewModel) { state ->
                if (state.onlyShowSpaces) {
                    sharedViewModel.handle(SpaceManagedSharedAction.CreateSpace)
                } else {
                    sharedViewModel.handle(SpaceManagedSharedAction.CreateRoom)
                }
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                SpaceAddRoomsViewEvents.WarnUnsavedChanged -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(CommonStrings.dialog_title_warning)
                            .setMessage(CommonStrings.warning_unsaved_change)
                            .setPositiveButton(CommonStrings.warning_unsaved_change_discard) { _, _ ->
                                sharedViewModel.handle(SpaceManagedSharedAction.HandleBack)
                            }
                            .setNegativeButton(CommonStrings.action_cancel, null)
                            .show()
                }
                is SpaceAddRoomsViewEvents.SaveFailed -> {
                    showErrorInSnackbar(it.reason)
                    invalidateOptionsMenu()
                }
                SpaceAddRoomsViewEvents.SavedDone -> {
                    sharedViewModel.handle(SpaceManagedSharedAction.HandleBack)
                }
            }
        }
    }

    override fun handlePrepareMenu(menu: Menu) {
        menu.findItem(R.id.spaceAddRoomSaveItem).isVisible = saveNeeded
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.spaceAddRoomSaveItem -> {
                viewModel.handle(SpaceAddRoomActions.Save)
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        views.roomList.cleanup()
        spaceEpoxyController.listener = null
        roomEpoxyController.listener = null
        dmEpoxyController.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        setupSpaceSection()
        setupRoomSection()
        setupDmSection()

        views.roomList.adapter = ConcatAdapter().apply {
            addAdapter(roomEpoxyController.adapter)
            addAdapter(spaceEpoxyController.adapter)
            addAdapter(dmEpoxyController.adapter)
        }
    }

    private fun setupSpaceSection() {
        spaceEpoxyController.sectionName = getString(CommonStrings.spaces_header)
        spaceEpoxyController.listener = this
        viewModel.spaceUpdatableLivePageResult.liveBoundaries.observe(viewLifecycleOwner) {
            spaceEpoxyController.boundaryChange(it)
        }
        viewModel.spaceUpdatableLivePageResult.livePagedList.observe(viewLifecycleOwner) {
            spaceEpoxyController.submitList(it)
        }
        listenItemCount(viewModel.spaceCountFlow) { spaceEpoxyController.totalSize = it }
    }

    private fun setupRoomSection() {
        roomEpoxyController.sectionName = getString(CommonStrings.rooms_header)
        roomEpoxyController.listener = this

        viewModel.roomUpdatableLivePageResult.liveBoundaries.observe(viewLifecycleOwner) {
            roomEpoxyController.boundaryChange(it)
        }
        viewModel.roomUpdatableLivePageResult.livePagedList.observe(viewLifecycleOwner) {
            roomEpoxyController.submitList(it)
        }
        listenItemCount(viewModel.roomCountFlow) { roomEpoxyController.totalSize = it }
        views.roomList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        views.roomList.setHasFixedSize(true)
    }

    private fun setupDmSection() {
        // This controller can be disabled depending on the space type (public or not)
        dmEpoxyController.sectionName = getString(CommonStrings.direct_chats_header)
        dmEpoxyController.listener = this
        viewModel.dmUpdatableLivePageResult.liveBoundaries.observe(viewLifecycleOwner) {
            dmEpoxyController.boundaryChange(it)
        }
        viewModel.dmUpdatableLivePageResult.livePagedList.observe(viewLifecycleOwner) {
            dmEpoxyController.submitList(it)
        }
        listenItemCount(viewModel.dmCountFlow) { dmEpoxyController.totalSize = it }
    }

    private fun listenItemCount(itemCountFlow: Flow<Int>, onEachAction: (Int) -> Unit) {
        lifecycleScope.launch {
            itemCountFlow
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collect { count -> onEachAction(count) }
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (viewModel.canGoBack()) {
            sharedViewModel.handle(SpaceManagedSharedAction.HandleBack)
        }
        return true
    }

    override fun onItemSelected(roomSummary: RoomSummary) {
        viewModel.handle(SpaceAddRoomActions.ToggleSelection(roomSummary))
    }
}
