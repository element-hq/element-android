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
import android.text.Spannable
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.beforeTextChangeEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.html.PillImageSpan
import kotlinx.android.synthetic.main.fragment_create_direct_room.*
import javax.inject.Inject

class CreateDirectRoomFragment : VectorBaseFragment(), CreateDirectRoomController.Callback {

    override fun getLayoutResId() = R.layout.fragment_create_direct_room

    override fun getMenuRes() = R.menu.vector_create_direct_room

    private val viewModel: CreateDirectRoomViewModel by activityViewModel()

    @Inject lateinit var directRoomController: CreateDirectRoomController
    @Inject lateinit var avatarRenderer: AvatarRenderer
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
            updateFilterViewWith(it)

        }
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
                .textChanges()
                .subscribe { text ->
                    val userMatches = MatrixPatterns.PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER.findAll(text)
                    val lastUserMatch = userMatches.lastOrNull()
                    val filterValue = if (lastUserMatch == null) {
                        text
                    } else {
                        text.substring(startIndex = lastUserMatch.range.endInclusive + 1)
                    }.trim()

                    val action = if (filterValue.isBlank()) {
                        CreateDirectRoomActions.ClearFilterKnownUsers
                    } else {
                        CreateDirectRoomActions.FilterKnownUsers(filterValue.toString())
                    }
                    viewModel.handle(action)
                }
                .disposeOnDestroy()

        createDirectRoomFilter
                .beforeTextChangeEvents()
                .subscribe { event ->
                    if (event.after == 0) {
                        val sub = event.text.substring(0, event.start)
                        val startIndexOfUser = sub.lastIndexOf(" ") + 1
                        val user = sub.substring(startIndexOfUser)
                        val selectedUser = withState(viewModel) { state ->
                            state.selectedUsers.find { it.userId == user }
                        }
                        if (selectedUser != null) {
                            viewModel.handle(CreateDirectRoomActions.RemoveSelectedUser(selectedUser))
                        }
                    }
                }
                .disposeOnDestroy()

        createDirectRoomFilter.requestFocus()
    }

    private fun setupCloseView() {
        createDirectRoomClose.setOnClickListener {
            requireActivity().finish()
        }
    }

    private fun renderState(state: CreateDirectRoomViewState) {
        directRoomController.setData(state)
    }

    private fun updateFilterViewWith(data: SelectUserAction) = withState(viewModel) { state ->
        if (state.selectedUsers.isEmpty()) {
            createDirectRoomFilter.text = null
        } else {
            val editable = createDirectRoomFilter.editableText
            val user = data.user
            if (data.isAdded) {
                val startIndex = editable.lastIndexOf(" ") + 1
                val endIndex = editable.length
                editable.replace(startIndex, endIndex, "${user.userId} ")
                val span = PillImageSpan(GlideApp.with(this), avatarRenderer, requireContext(), user.userId, user)
                span.bind(createDirectRoomFilter)
                editable.setSpan(span, startIndex, startIndex + user.userId.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val startIndex = editable.indexOf(user.userId)
                if (startIndex != -1) {
                    var endIndex = editable.indexOf(" ", startIndex) + 1
                    if (endIndex == 0) {
                        endIndex = editable.length
                    }
                    editable.replace(startIndex, endIndex, "")
                }
            }
        }
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(CreateDirectRoomActions.SelectUser(user))
    }
}