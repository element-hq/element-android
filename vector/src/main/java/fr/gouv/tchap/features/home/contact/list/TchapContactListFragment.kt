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
import im.vector.app.databinding.FragmentTchapContactListBinding
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class TchapContactListFragment @Inject constructor(
        private val tchapContactListController: TchapContactListController,
) : VectorBaseFragment<FragmentTchapContactListBinding>(),
        TchapContactListController.Callback {

    private val args: TchapContactListFragmentArgs by args()
    private val viewModel: TchapContactListViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapContactListBinding {
        return FragmentTchapContactListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        views.userListFilterGroup.isVisible = args.showFilter
        views.userListSearch
                .textChanges()
                .startWith(views.userListSearch.text)
                .subscribe { text ->
                    searchContactsWith(text.trim().toString())
                }
                .disposeOnDestroyView()

        views.userListSearch.setupAsSearch()
    }

    fun searchContactsWith(value: String) {
        val action = if (value.isBlank()) {
            TchapContactListAction.ClearSearchUsers
        } else {
            TchapContactListAction.SearchUsers(value)
        }
        viewModel.handle(action)
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
