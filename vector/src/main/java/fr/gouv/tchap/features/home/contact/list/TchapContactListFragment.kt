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
import android.widget.Toast
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding3.widget.textChanges
import fr.gouv.tchap.core.dialogs.InviteByEmailDialog
import fr.gouv.tchap.features.userdirectory.TchapContactListSharedAction
import fr.gouv.tchap.features.userdirectory.TchapContactListSharedActionViewModel
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
import im.vector.app.features.userdirectory.PendingSelection
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class TchapContactListFragment @Inject constructor(
        private val tchapContactListController: TchapContactListController
) : VectorBaseFragment<FragmentTchapContactListBinding>(),
        TchapContactListController.Callback {

    private val args: TchapContactListFragmentArgs by args()
    private val viewModel: TchapContactListViewModel by activityViewModel()
    private lateinit var sharedActionViewModel: TchapContactListSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapContactListBinding {
        return FragmentTchapContactListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(TchapContactListSharedActionViewModel::class.java)

        setupRecyclerView()
        setupSearchView()
        setupUserConsent()
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

    private fun setupUserConsent() = withState(viewModel) {
        if (!it.userConsent) {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.permissions_rationale_popup_title)
                    .setMessage(R.string.permissions_rationale_msg_contacts)
                    .setOnCancelListener { Toast.makeText(activity, R.string.missing_permissions_warning, Toast.LENGTH_SHORT).show() }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        viewModel.handle(TchapContactListAction.SetUserConsent)
                        checkPermission()
                    }
                    .show()
        } else {
            checkPermission()
        }
    }

    private fun checkPermission() {
        if (checkPermissions(PERMISSIONS_FOR_MEMBERS_SEARCH, requireActivity(), loadContactsActivityResultLauncher,
                        R.string.permissions_rationale_msg_contacts)) {
            viewModel.handle(TchapContactListAction.LoadContacts)
        }
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

    override fun onInviteByEmailClick() {
        InviteByEmailDialog(requireActivity()).show(
                object : InviteByEmailDialog.Listener {
                    override fun inviteByEmail(email: String) {
                        viewModel.handle(TchapContactListAction.AddPendingSelection(PendingSelection.ThreePidPendingSelection(ThreePid.Email(email))))
                        sharedActionViewModel.post(TchapContactListSharedAction.OnInviteByEmail(email))
                    }
                }
        )
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        sharedActionViewModel.post(TchapContactListSharedAction.OnSelectContact(user))
//        viewModel.handle(TchapContactListAction.AddPendingSelection(PendingSelection.UserPendingSelection(user)))
    }

    override fun onMatrixIdClick(matrixId: String) {
        view?.hideKeyboard()
        viewModel.handle(TchapContactListAction.CancelSearch)
//        viewModel.handle(TchapContactListAction.AddPendingSelection(PendingSelection.UserPendingSelection(User(matrixId))))
    }

    override fun onContactSearchClick() {
        viewModel.handle(TchapContactListAction.OpenSearch)
    }

    private val loadContactsActivityResultLauncher = registerForPermissionsResult { allGranted, _ ->
        if (allGranted) {
            viewModel.handle(TchapContactListAction.LoadContacts)
        }
    }
}
