/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.userdirectory

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_create_direct_room_directory_users.recyclerView
import kotlinx.android.synthetic.main.fragment_user_directory.*
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class UserDirectoryFragment @Inject constructor(
        private val directRoomController: DirectoryUsersController
) : VectorBaseFragment(), DirectoryUsersController.Callback {

    override fun getLayoutResId() = R.layout.fragment_user_directory
    private val viewModel: UserDirectoryViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: UserDirectorySharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserDirectorySharedActionViewModel::class.java)
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
        userDirectorySearchById.setupAsSearch(searchIconRes = 0)
        userDirectorySearchById
                .textChanges()
                .subscribe {
                    viewModel.handle(UserDirectoryAction.SearchDirectoryUsers(it.toString()))
                }
                .disposeOnDestroyView()
        userDirectorySearchById.showKeyboard(andRequestFocus = true)
    }

    private fun setupCloseView() {
        userDirectoryClose.debouncedClicks {
            sharedActionViewModel.post(UserDirectorySharedAction.GoBack)
        }
    }

    override fun invalidate() = withState(viewModel) {
        directRoomController.setData(it)
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(UserDirectoryAction.SelectPendingInvitee(PendingInvitee.UserPendingInvitee(user)))
        sharedActionViewModel.post(UserDirectorySharedAction.GoBack)
    }

    override fun retryDirectoryUsersRequest() {
        val currentSearch = userDirectorySearchById.text.toString()
        viewModel.handle(UserDirectoryAction.SearchDirectoryUsers(currentSearch))
    }
}
