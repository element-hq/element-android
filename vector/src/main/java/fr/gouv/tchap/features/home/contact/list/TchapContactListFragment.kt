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

package fr.gouv.tchap.features.home.contact.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_MEMBERS_SEARCH
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.FragmentUserListBinding
import im.vector.app.features.homeserver.HomeServerCapabilitiesViewModel
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class TchapContactListFragment @Inject constructor(
        private val tchapContactListController: TchapContactListController,
        val homeServerCapabilitiesViewModelFactory: HomeServerCapabilitiesViewModel.Factory
) : VectorBaseFragment<FragmentUserListBinding>(),
        TchapContactListController.Callback {

    private val args: TchapContactListFragmentArgs by args()
    private val viewModel: TchapContactListViewModel by activityViewModel()
    private val homeServerCapabilitiesViewModel: HomeServerCapabilitiesViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUserListBinding {
        return FragmentUserListBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = args.menuResId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.showToolbar) {
            views.userListTitle.text = args.title
            vectorBaseActivity.setSupportActionBar(views.userListToolbar)
            setupCloseView()
            views.userListToolbar.isVisible = true
        } else {
            views.userListToolbar.isVisible = false
        }
        setupRecyclerView()
        setupSearchView()

        if (checkPermissions(PERMISSIONS_FOR_MEMBERS_SEARCH, requireActivity(), loadContactsActivityResultLauncher, R.string.permissions_rationale_msg_contacts)) {
            viewModel.handle(TchapContactListAction.LoadContacts)
        }
    }

    override fun onDestroyView() {
        views.userListRecyclerView.cleanup()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        tchapContactListController.callback = this
        // Don't activate animation as we might have way to much item animation when filtering
        views.userListRecyclerView.configureWith(tchapContactListController, disableItemAnimation = true)
    }

    private fun setupSearchView() {
        withState(viewModel) {
            views.userListSearch.hint = getString(R.string.user_directory_search_hint)
        }
        views.userListSearch
                .textChanges()
                .startWith(views.userListSearch.text)
                .subscribe { text ->
                    val searchValue = text.trim()
                    val action = if (searchValue.isBlank()) {
                        TchapContactListAction.ClearSearchUsers
                    } else {
                        TchapContactListAction.SearchUsers(searchValue.toString())
                    }
                    viewModel.handle(action)
                }
                .disposeOnDestroyView()

        views.userListSearch.setupAsSearch()
        views.userListSearch.requestFocus()
    }

    private fun setupCloseView() {
        views.userListClose.debouncedClicks {
            requireActivity().finish()
        }
    }

    override fun invalidate() = withState(viewModel) {
        tchapContactListController.setData(it)
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
//        viewModel.handle(TchapContactListAction.AddPendingSelection(PendingSelection.UserPendingSelection(user)))
    }

    override fun onMatrixIdClick(matrixId: String) {
        view?.hideKeyboard()
//        viewModel.handle(TchapContactListAction.AddPendingSelection(PendingSelection.UserPendingSelection(User(matrixId))))
    }

    private val loadContactsActivityResultLauncher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            viewModel.handle(TchapContactListAction.LoadContacts)
        }
    }
}
