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

package im.vector.app.features.contactsbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.showIdentityServerConsentDialog
import im.vector.app.databinding.FragmentContactsBookBinding
import im.vector.app.features.userdirectory.PendingSelection
import im.vector.app.features.userdirectory.UserListAction
import im.vector.app.features.userdirectory.UserListSharedAction
import im.vector.app.features.userdirectory.UserListSharedActionViewModel
import im.vector.app.features.userdirectory.UserListViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import reactivecircus.flowbinding.android.widget.checkedChanges
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class ContactsBookFragment @Inject constructor(
        private val contactsBookController: ContactsBookController
) : VectorBaseFragment<FragmentContactsBookBinding>(), ContactsBookController.Callback {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentContactsBookBinding {
        return FragmentContactsBookBinding.inflate(inflater, container, false)
    }

    private val viewModel: UserListViewModel by activityViewModel()

    // Use activityViewModel to avoid loading several times the data
    private val contactsBookViewModel: ContactsBookViewModel by activityViewModel()

    private lateinit var sharedActionViewModel: UserListSharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserListSharedActionViewModel::class.java)
        setupRecyclerView()
        setupFilterView()
        setupConsentView()
        setupOnlyBoundContactsView()
        setupToolbar(views.phoneBookToolbar)
                .allowBack(useCross = true)
        contactsBookViewModel.observeViewEvents {
            when (it) {
                is ContactsBookViewEvents.Failure             -> showFailure(it.throwable)
                is ContactsBookViewEvents.OnPoliciesRetrieved -> showConsentDialog(it)
            }
        }
    }

    private fun setupConsentView() {
        views.phoneBookSearchForMatrixContacts.debouncedClicks {
            contactsBookViewModel.handle(ContactsBookAction.UserConsentRequest)
        }
    }

    private fun showConsentDialog(event: ContactsBookViewEvents.OnPoliciesRetrieved) {
        requireContext().showIdentityServerConsentDialog(
                event.identityServerWithTerms,
                consentCallBack = { contactsBookViewModel.handle(ContactsBookAction.UserConsentGranted) }
        )
    }

    private fun setupOnlyBoundContactsView() {
        views.phoneBookOnlyBoundContacts.checkedChanges()
                .onEach {
                    contactsBookViewModel.handle(ContactsBookAction.OnlyBoundContacts(it))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupFilterView() {
        views.phoneBookFilter
                .textChanges()
                .skipInitialValue()
                .debounce(300)
                .onEach {
                    contactsBookViewModel.handle(ContactsBookAction.FilterWith(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        views.phoneBookRecyclerView.cleanup()
        contactsBookController.callback = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        contactsBookController.callback = this
        views.phoneBookRecyclerView.configureWith(contactsBookController)
    }

    override fun invalidate() = withState(contactsBookViewModel) { state ->
        views.phoneBookSearchForMatrixContacts.isVisible = state.filteredMappedContacts.isNotEmpty() && state.identityServerUrl != null && !state.userConsent
        views.phoneBookOnlyBoundContacts.isVisible = state.isBoundRetrieved
        contactsBookController.setData(state)
    }

    override fun onMatrixIdClick(matrixId: String) {
        view?.hideKeyboard()
        viewModel.handle(UserListAction.AddPendingSelection(PendingSelection.UserPendingSelection(User(matrixId))))
        sharedActionViewModel.post(UserListSharedAction.GoBack)
    }

    override fun onThreePidClick(threePid: ThreePid) {
        view?.hideKeyboard()
        viewModel.handle(UserListAction.AddPendingSelection(PendingSelection.ThreePidPendingSelection(threePid)))
        sharedActionViewModel.post(UserListSharedAction.GoBack)
    }
}
