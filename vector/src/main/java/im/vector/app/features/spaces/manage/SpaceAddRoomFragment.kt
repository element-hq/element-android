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

package im.vector.app.features.spaces.manage

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyViewHolder
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceAddRoomsBinding
import im.vector.app.features.home.room.list.RoomCategoryItem_
import io.reactivex.rxkotlin.subscribeBy
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SpaceAddRoomFragment @Inject constructor(
        private val spaceEpoxyController: AddRoomListController,
        private val roomEpoxyController: AddRoomListController,
        private val viewModelFactory: SpaceAddRoomsViewModel.Factory
) : VectorBaseFragment<FragmentSpaceAddRoomsBinding>(),
        OnBackPressed, AddRoomListController.Listener, SpaceAddRoomsViewModel.Factory {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceAddRoomsBinding.inflate(layoutInflater, container, false)

    private val viewModel by fragmentViewModel(SpaceAddRoomsViewModel::class)

    private val sharedViewModel: SpaceManageSharedViewModel by activityViewModel()

    override fun create(initialState: SpaceAddRoomsState): SpaceAddRoomsViewModel =
            viewModelFactory.create(initialState)

    override fun getMenuRes(): Int = R.menu.menu_space_add_room

    private var saveNeeded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(views.addRoomToSpaceToolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

//        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()

        views.publicRoomsFilter.queryTextChanges()
                .debounce(100, TimeUnit.MILLISECONDS)
                .subscribeBy {
                    viewModel.handle(SpaceAddRoomActions.UpdateFilter(it.toString()))
                }
                .disposeOnDestroyView()

        viewModel.selectionListLiveData.observe(viewLifecycleOwner) {
            spaceEpoxyController.selectedItems = it
            roomEpoxyController.selectedItems = it
            saveNeeded = it.values.any { it }
            invalidateOptionsMenu()
        }

        viewModel.selectSubscribe(this, SpaceAddRoomsState::spaceName) {
            views.appBarSpaceInfo.text = it
        }.disposeOnDestroyView()

        viewModel.selectSubscribe(this, SpaceAddRoomsState::ignoreRooms) {
            spaceEpoxyController.ignoreRooms = it
            roomEpoxyController.ignoreRooms = it
        }.disposeOnDestroyView()

        viewModel.selectSubscribe(this, SpaceAddRoomsState::isSaving) {
            if (it is Loading) {
                sharedViewModel.handle(SpaceManagedSharedAction.ShowLoading)
            } else {
                sharedViewModel.handle(SpaceManagedSharedAction.HideLoading)
            }
        }.disposeOnDestroyView()

        views.createNewRoom.debouncedClicks {
            sharedViewModel.handle(SpaceManagedSharedAction.CreateRoom)
        }

        viewModel.observeViewEvents {
            when (it) {
                SpaceAddRoomsViewEvents.WarnUnsavedChanged -> {
                    AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.warning_unsaved_change)
                            .setPositiveButton(R.string.warning_unsaved_change_discard) { _, _ ->
                                sharedViewModel.handle(SpaceManagedSharedAction.HandleBack)
                            }
                            .setNegativeButton(R.string.cancel, null)
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.spaceAddRoomSaveItem).isVisible = saveNeeded
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.spaceAddRoomSaveItem) {
            viewModel.handle(SpaceAddRoomActions.Save)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        views.roomList.cleanup()
        spaceEpoxyController.listener = null
        roomEpoxyController.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter()
        spaceEpoxyController.sectionName = getString(R.string.spaces_header)
        roomEpoxyController.sectionName = getString(R.string.rooms_header)
        spaceEpoxyController.listener = this
        roomEpoxyController.listener = this

        viewModel.updatableLiveSpacePageResult.liveBoundaries.observe(viewLifecycleOwner) {
            spaceEpoxyController.boundaryChange(it)
        }
        viewModel.updatableLiveSpacePageResult.livePagedList.observe(viewLifecycleOwner) {
            spaceEpoxyController.totalSize = it.size
            spaceEpoxyController.submitList(it)
        }

        viewModel.updatableLivePageResult.liveBoundaries.observe(viewLifecycleOwner) {
            roomEpoxyController.boundaryChange(it)
        }
        viewModel.updatableLivePageResult.livePagedList.observe(viewLifecycleOwner) {
            roomEpoxyController.totalSize = it.size
            roomEpoxyController.submitList(it)
        }

        views.roomList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        views.roomList.addItemDecoration(

                object : DividerItemDecoration(context, VERTICAL) {
                    val decorationDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider_horizontal)

                    override fun getDrawable(): Drawable? {
                        return decorationDrawable
                    }

                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        val position = parent.getChildAdapterPosition(view)
                        val vh = parent.findViewHolderForAdapterPosition(position)
                        val nextIsSectionOrFinal = parent.findViewHolderForAdapterPosition(position + 1)?.let {
                            (it as? EpoxyViewHolder)?.model is RoomCategoryItem_
                        } ?: true
                        if (vh == null
                                || (vh as? EpoxyViewHolder)?.model is RoomCategoryItem_
                                || nextIsSectionOrFinal
                        ) {
                            outRect.setEmpty()
                        } else {
                            super.getItemOffsets(outRect, view, parent, state)
                        }
                    }

                    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                        super.onDraw(c, parent, state)
                    }
                }
        )
        views.roomList.setHasFixedSize(true)

        concatAdapter.addAdapter(roomEpoxyController.adapter)
        concatAdapter.addAdapter(spaceEpoxyController.adapter)

        views.roomList.adapter = concatAdapter
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
