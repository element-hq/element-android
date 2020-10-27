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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.invite.InviteUsersToRoomActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.FoundThreePid
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit

private typealias KnownUsersSearch = String
private typealias DirectoryUsersSearch = String
private typealias ContactUsersSearch = String

class UserListViewModel @AssistedInject constructor(@Assisted
                                                    initialState: UserListViewState,
                                                    private val contactsDataSource: ContactsDataSource,
                                                    private val session: Session)
    : VectorViewModel<UserListViewState, UserListAction, UserListViewEvents>(initialState) {

    private val knownUsersSearch = BehaviorRelay.create<KnownUsersSearch>()
    private val directoryUsersSearch = BehaviorRelay.create<DirectoryUsersSearch>()
    private val contactUsersSearch = BehaviorRelay.create<ContactUsersSearch>()

    private var allContacts: List<MappedContact> = emptyList()
    private var mappedContacts: List<MappedContact> = emptyList()

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: UserListViewState): UserListViewModel
    }

    companion object : MvRxViewModelFactory<UserListViewModel, UserListViewState> {
        override fun create(viewModelContext: ViewModelContext, state: UserListViewState): UserListViewModel? {
            return when (viewModelContext) {
                is ActivityViewModelContext -> {
                    when (viewModelContext.activity<FragmentActivity>()) {
                        is CreateDirectRoomActivity  -> (viewModelContext.activity<CreateDirectRoomActivity>()).userListViewModelFactory.create(state)
                        is InviteUsersToRoomActivity -> (viewModelContext.activity<InviteUsersToRoomActivity>()).userListViewModelFactory.create(state)
                        else                         -> error("Wrong activity or fragment")
                    }
                }
                is FragmentViewModelContext -> (viewModelContext.fragment() as UserListFragment).userListViewModelFactory.create(state)
                else                        -> error("Wrong activity or fragment")
            }
        }
    }

    init {
        observeUsers()

        selectSubscribe(UserListViewState::onlyBoundContacts) {
            updateFilteredMappedContacts()
        }
    }

    override fun handle(action: UserListAction) {
        when (action) {
            is UserListAction.SearchUsers                     -> handleSearchUsers(action.value)
            is UserListAction.ClearSearchUsers                -> handleClearSearchUsers()
            is UserListAction.SelectPendingInvitee            -> handleSelectUser(action)
            is UserListAction.RemovePendingInvitee            -> handleRemoveSelectedUser(action)
            is UserListAction.OnlyBoundContacts               -> handleOnlyBoundContacts(action)
            is UserListAction.OnReadContactsPermissionGranted -> loadContacts()
        }.exhaustive
    }

    private fun handleSearchUsers(searchTerm: String) {
        setState {
            copy(searchTerm = searchTerm)
        }
        knownUsersSearch.accept(searchTerm)
        directoryUsersSearch.accept(searchTerm)
        contactUsersSearch.accept(searchTerm)
    }

    private fun handleClearSearchUsers() {
        knownUsersSearch.accept("")
        directoryUsersSearch.accept("")
        contactUsersSearch.accept("")
    }

    private fun observeUsers() = withState { state ->
        knownUsersSearch
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    session.rx().livePagedUsers(it, state.excludedUserIds)
                }
                .execute { async ->
                    copy(knownUsers = async)
                }

        directoryUsersSearch
                .debounce(300, TimeUnit.MILLISECONDS)
                .switchMapSingle { search ->
                    val stream = if (search.isBlank()) {
                        Single.just(emptyList())
                    } else {
                        session.rx()
                                .searchUsersDirectory(search, 50, state.excludedUserIds ?: emptySet())
                                .map { users ->
                                    users.sortedBy { it.toMatrixItem().firstLetterOfDisplayName() }
                                }
                    }
                    stream.toAsync {
                        copy(directoryUsers = it)
                    }
                }
                .subscribe()
                .disposeOnClear()

        contactUsersSearch
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map { search ->
                    if (search.isEmpty()) {
                        emptyList()
                    } else {
                        allContacts.filter { it.displayName.contains(search) }
                    }
                }
                .execute { mappedContacts ->
                    copy(mappedContacts = mappedContacts)
                            .also {
                                mappedContacts()?.let {
                                    performLookup(it)
                                }
                            }
                }
    }

    private fun performLookup(contactList: List<MappedContact>) {
        val threePids = contactList.flatMap { contact: MappedContact ->
            contact.emails.map { ThreePid.Email(it.email) } +
                    contact.msisdns.map { ThreePid.Msisdn(it.phoneNumber) }
        }
        session.identityService().lookUp(threePids, object : MatrixCallback<List<FoundThreePid>> {
            override fun onFailure(failure: Throwable) {
                // Ignore
                Timber.w(failure, "Unable to perform the lookup")
                updateFilteredMappedContacts()
            }

            override fun onSuccess(data: List<FoundThreePid>) {
                mappedContacts = allContacts.map { contactModel ->
                    contactModel.copy(
                            emails = contactModel.emails.map { email ->
                                email.copy(
                                        matrixId = data
                                                .firstOrNull { foundThreePid -> foundThreePid.threePid.value == email.email }
                                                ?.matrixId
                                )
                            },
                            msisdns = contactModel.msisdns.map { msisdn ->
                                msisdn.copy(
                                        matrixId = data
                                                .firstOrNull { foundThreePid -> foundThreePid.threePid.value == msisdn.phoneNumber }
                                                ?.matrixId
                                )
                            }
                    )
                }

                setState {
                    copy(isBoundRetrieved = true)
                }

                updateFilteredMappedContacts()
            }
        })
    }

    private fun loadContacts() {
        setState {
            copy(
                    mappedContacts = Loading()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            allContacts = contactsDataSource.getContacts(
                    withEmails = true,
                    // Do not handle phone numbers for the moment
                    withMsisdn = false
            )
            mappedContacts = allContacts

            setState {
                copy(
                        mappedContacts = Success(allContacts)
                )
            }

            performLookup(allContacts)
        }
    }

    private fun updateFilteredMappedContacts() = withState { state ->
        val filteredMappedContacts = mappedContacts
                .filter { it.displayName.contains(state.searchTerm, true) }
                .filter { contactModel ->
                    !state.onlyBoundContacts
                            || contactModel.emails.any { it.matrixId != null } || contactModel.msisdns.any { it.matrixId != null }
                }

        setState {
            copy(
                    filteredMappedContacts = filteredMappedContacts
            )
        }
    }

    private fun handleSelectUser(action: UserListAction.SelectPendingInvitee) = withState { state ->
        // Reset the filter asap
        directoryUsersSearch.accept("")
        val selectedUsers = state.pendingInvitees.toggle(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }

    private fun handleRemoveSelectedUser(action: UserListAction.RemovePendingInvitee) = withState { state ->
        val selectedUsers = state.pendingInvitees.minus(action.pendingInvitee)
        setState { copy(pendingInvitees = selectedUsers) }
    }

    private fun handleOnlyBoundContacts(action: UserListAction.OnlyBoundContacts) {
        setState {
            copy(
                    onlyBoundContacts = action.onlyBoundContacts
            )
        }
    }
}
