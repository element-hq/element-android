/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class ContactsBookFragment :
        VectorBaseFragment<FragmentContactsBookBinding>(),
        ContactsBookController.Callback {

    @Inject lateinit var contactsBookController: ContactsBookController

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
                is ContactsBookViewEvents.Failure -> showFailure(it.throwable)
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
