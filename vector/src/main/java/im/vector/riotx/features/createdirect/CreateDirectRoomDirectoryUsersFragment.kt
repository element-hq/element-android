/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.createdirect

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.setupAsSearch
import im.vector.riotx.core.extensions.showKeyboard
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_create_direct_room_directory_users.*
import javax.inject.Inject

class CreateDirectRoomDirectoryUsersFragment @Inject constructor(
        private val directRoomController: DirectoryUsersController
) : VectorBaseFragment(), DirectoryUsersController.Callback {

    override fun getLayoutResId() = R.layout.fragment_create_direct_room_directory_users

    private val viewModel: CreateDirectRoomViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: CreateDirectRoomSharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(CreateDirectRoomSharedActionViewModel::class.java)
        setupRecyclerView()
        setupSearchByMatrixIdView()
        setupCloseView()
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        directRoomController.callback = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        directRoomController.callback = this
        recyclerView.configureWith(directRoomController)
    }

    private fun setupSearchByMatrixIdView() {
        createDirectRoomSearchById.setupAsSearch(searchIconRes = 0)
        createDirectRoomSearchById
                .textChanges()
                .subscribe {
                    viewModel.handle(CreateDirectRoomAction.SearchDirectoryUsers(it.toString()))
                }
                .disposeOnDestroyView()
        createDirectRoomSearchById.showKeyboard(andRequestFocus = true)
    }

    private fun setupCloseView() {
        createDirectRoomClose.setOnClickListener {
            sharedActionViewModel.post(CreateDirectRoomSharedAction.GoBack)
        }
    }

    override fun invalidate() = withState(viewModel) {
        directRoomController.setData(it)
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(CreateDirectRoomAction.SelectUser(user))
        sharedActionViewModel.post(CreateDirectRoomSharedAction.GoBack)
    }

    override fun retryDirectoryUsersRequest() {
        val currentSearch = createDirectRoomSearchById.text.toString()
        viewModel.handle(CreateDirectRoomAction.SearchDirectoryUsers(currentSearch))
    }
}
