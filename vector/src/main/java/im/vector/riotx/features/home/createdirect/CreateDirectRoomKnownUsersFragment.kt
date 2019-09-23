/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import androidx.core.view.size
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.extensions.setupAsSearch
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.home.AvatarRenderer
import kotlinx.android.synthetic.main.fragment_create_direct_room.*
import javax.inject.Inject

class CreateDirectRoomKnownUsersFragment : VectorBaseFragment(), KnownUsersController.Callback {

    override fun getLayoutResId() = R.layout.fragment_create_direct_room

    override fun getMenuRes() = R.menu.vector_create_direct_room

    private val viewModel: CreateDirectRoomViewModel by activityViewModel()

    @Inject lateinit var directRoomController: KnownUsersController
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var dimensionConverter: DimensionConverter
    private lateinit var navigationViewModel: CreateDirectRoomNavigationViewModel

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        navigationViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(CreateDirectRoomNavigationViewModel::class.java)
        vectorBaseActivity.setSupportActionBar(createDirectRoomToolbar)
        setupRecyclerView()
        setupFilterView()
        setupAddByMatrixIdView()
        setupCloseView()
        viewModel.selectUserEvent.observeEvent(this) {
            updateChipsView(it)
        }
        viewModel.selectSubscribe(this, CreateDirectRoomViewState::selectedUsers) {
            renderSelectedUsers(it)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) {
            val createMenuItem = menu.findItem(R.id.action_create_direct_room)
            val showMenuItem = it.selectedUsers.isNotEmpty()
            createMenuItem.setVisible(showMenuItem)
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_direct_room -> {
                viewModel.handle(CreateDirectRoomActions.CreateRoomAndInviteSelectedUsers)
                true
            }
            else                           ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun setupAddByMatrixIdView() {
        addByMatrixId.setOnClickListener {
            navigationViewModel.goTo(CreateDirectRoomActivity.Navigation.UsersDirectory)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        // Don't activate animation as we might have way to much item animation when filtering
        recyclerView.itemAnimator = null
        directRoomController.callback = this
        recyclerView.setController(directRoomController)
    }

    private fun setupFilterView() {
        createDirectRoomFilter
                .textChanges()
                .startWith(createDirectRoomFilter.text)
                .subscribe { text ->
                    val filterValue = text.trim()
                    val action = if (filterValue.isBlank()) {
                        CreateDirectRoomActions.ClearFilterKnownUsers
                    } else {
                        CreateDirectRoomActions.FilterKnownUsers(filterValue.toString())
                    }
                    viewModel.handle(action)
                }
                .disposeOnDestroy()

        createDirectRoomFilter.setupAsSearch()
        createDirectRoomFilter.requestFocus()
    }

    private fun setupCloseView() {
        createDirectRoomClose.setOnClickListener {
            requireActivity().finish()
        }
    }

    override fun invalidate() = withState(viewModel) {
        directRoomController.setData(it)
    }

    private fun updateChipsView(data: SelectUserAction) {
        if (data.isAdded) {
            addChipToGroup(data.user, chipGroup)
        } else {
            if (chipGroup.size > data.index) {
                chipGroup.removeViewAt(data.index)
            }
        }
    }

    private fun renderSelectedUsers(selectedUsers: Set<User>) {
        vectorBaseActivity.invalidateOptionsMenu()
        if (selectedUsers.isNotEmpty() && chipGroup.size == 0) {
            selectedUsers.forEach { addChipToGroup(it, chipGroup) }
        }
    }

    private fun addChipToGroup(user: User, chipGroup: ChipGroup) {
        val chip = Chip(requireContext())
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = dimensionConverter.dpToPx(1).toFloat()
        chip.text = if (user.displayName.isNullOrBlank()) user.userId else user.displayName
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chipGroup.addView(chip)
        chip.setOnCloseIconClickListener {
            viewModel.handle(CreateDirectRoomActions.RemoveSelectedUser(user))
        }
        chipGroupScrollView.post {
            chipGroupScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(CreateDirectRoomActions.SelectUser(user))
    }
}