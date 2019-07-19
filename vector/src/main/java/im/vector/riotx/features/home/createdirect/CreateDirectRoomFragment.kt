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
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.activityViewModel
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import kotlinx.android.synthetic.main.fragment_create_direct_room.*
import kotlinx.android.synthetic.main.fragment_public_rooms.*
import javax.inject.Inject

class CreateDirectRoomFragment : VectorBaseFragment(), CreateDirectRoomController.Callback {

    override fun getLayoutResId() = R.layout.fragment_create_direct_room

    override fun getMenuRes() = R.menu.vector_create_direct_room

    private val viewModel: CreateDirectRoomViewModel by activityViewModel()

    @Inject lateinit var directRoomController: CreateDirectRoomController
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
        viewModel.subscribe(this) { renderState(it) }
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
        directRoomController.displayMode = CreateDirectRoomViewState.DisplayMode.KNOWN_USERS
        recyclerView.setController(directRoomController)
    }

    private fun setupFilterView() {
        createDirectRoomFilter
                .queryTextChanges()
                .subscribe {
                    val action = if (it.isNullOrEmpty()) {
                        CreateDirectRoomActions.ClearFilterKnownUsers
                    } else {
                        CreateDirectRoomActions.FilterKnownUsers(it.toString())
                    }
                    viewModel.handle(action)
                }
                .disposeOnDestroy()
    }

    private fun setupCloseView() {
        createDirectRoomClose.setOnClickListener {
            requireActivity().finish()
        }
    }

    private fun renderState(state: CreateDirectRoomViewState) {

        directRoomController.setData(state)
    }

    override fun onItemClick(user: User) {
        viewModel.handle(CreateDirectRoomActions.SelectUser(user))
    }
}