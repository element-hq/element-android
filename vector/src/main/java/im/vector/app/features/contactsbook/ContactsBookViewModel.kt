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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.invite.InviteUsersToRoomActivity
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.FoundThreePid
import org.matrix.android.sdk.api.session.identity.ThreePid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

private typealias PhoneBookSearch = String

class ContactsBookViewModel @AssistedInject constructor(@Assisted
                                                     initialState: ContactsBookViewState,
                                                        private val contactsDataSource: ContactsDataSource,
                                                        private val session: Session)
    : VectorViewModel<ContactsBookViewState, ContactsBookAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: ContactsBookViewState): ContactsBookViewModel
    }

    companion object : MvRxViewModelFactory<ContactsBookViewModel, ContactsBookViewState> {

        override fun create(viewModelContext: ViewModelContext, state: ContactsBookViewState): ContactsBookViewModel? {
            return when (viewModelContext) {
                is FragmentViewModelContext -> (viewModelContext.fragment() as ContactsBookFragment).contactsBookViewModelFactory.create(state)
                is ActivityViewModelContext -> {
                    when (viewModelContext.activity<FragmentActivity>()) {
                        is CreateDirectRoomActivity  -> viewModelContext.activity<CreateDirectRoomActivity>().contactsBookViewModelFactory.create(state)
                        is InviteUsersToRoomActivity -> viewModelContext.activity<InviteUsersToRoomActivity>().contactsBookViewModelFactory.create(state)
                        else                         -> error("Wrong activity or fragment")
                    }
                }
                else                        -> error("Wrong activity or fragment")
            }
        }
    }

    private var allContacts: List<MappedContact> = emptyList()
    private var mappedContacts: List<MappedContact> = emptyList()

    init {
        loadContacts()

        selectSubscribe(ContactsBookViewState::searchTerm, ContactsBookViewState::onlyBoundContacts) { _, _ ->
            updateFilteredMappedContacts()
        }
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
            updateFilteredMappedContacts()
        }
    }

    private fun performLookup(data: List<MappedContact>) {
        viewModelScope.launch {
            val threePids = data.flatMap { contact ->
                contact.emails.map { ThreePid.Email(it.email) } +
                        contact.msisdns.map { ThreePid.Msisdn(it.phoneNumber) }
            }
            session.identityService().lookUp(threePids, object : MatrixCallback<List<FoundThreePid>> {
                override fun onFailure(failure: Throwable) {
                    // Ignore
                    Timber.w(failure, "Unable to perform the lookup")
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
                        copy(
                                isBoundRetrieved = true
                        )
                    }

                    updateFilteredMappedContacts()
                }
            })
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

    override fun handle(action: ContactsBookAction) {
        when (action) {
            is ContactsBookAction.FilterWith        -> handleFilterWith(action)
            is ContactsBookAction.OnlyBoundContacts -> handleOnlyBoundContacts(action)
        }.exhaustive
    }

    private fun handleOnlyBoundContacts(action: ContactsBookAction.OnlyBoundContacts) {
        setState {
            copy(
                    onlyBoundContacts = action.onlyBoundContacts
            )
        }
    }

    private fun handleFilterWith(action: ContactsBookAction.FilterWith) {
        setState {
            copy(
                    searchTerm = action.filter
            )
        }
    }
}
