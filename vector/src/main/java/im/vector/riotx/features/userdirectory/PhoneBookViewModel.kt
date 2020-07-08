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

package im.vector.riotx.features.userdirectory

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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.identity.FoundThreePid
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.riotx.core.contacts.ContactModel
import im.vector.riotx.core.contacts.ContactsDataSource
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.createdirect.CreateDirectRoomActivity
import im.vector.riotx.features.invite.InviteUsersToRoomActivity
import kotlinx.coroutines.launch

private typealias PhoneBookSearch = String

class PhoneBookViewModel @AssistedInject constructor(@Assisted
                                                     initialState: PhoneBookViewState,
                                                     private val contactsDataSource: ContactsDataSource,
                                                     private val session: Session)
    : VectorViewModel<PhoneBookViewState, PhoneBookAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: PhoneBookViewState): PhoneBookViewModel
    }

    companion object : MvRxViewModelFactory<PhoneBookViewModel, PhoneBookViewState> {

        override fun create(viewModelContext: ViewModelContext, state: PhoneBookViewState): PhoneBookViewModel? {
            return when (viewModelContext) {
                is FragmentViewModelContext -> (viewModelContext.fragment() as PhoneBookFragment).phoneBookViewModelFactory.create(state)
                is ActivityViewModelContext -> {
                    when (viewModelContext.activity<FragmentActivity>()) {
                        is CreateDirectRoomActivity  -> viewModelContext.activity<CreateDirectRoomActivity>().phoneBookViewModelFactory.create(state)
                        is InviteUsersToRoomActivity -> viewModelContext.activity<InviteUsersToRoomActivity>().phoneBookViewModelFactory.create(state)
                        else                         -> error("Wrong activity or fragment")
                    }
                }
                else                        -> error("Wrong activity or fragment")
            }
        }
    }

    private var allContacts: List<ContactModel> = emptyList()
    private var mappedContacts: List<ContactModel> = emptyList()
    private var foundThreePid: List<FoundThreePid> = emptyList()

    init {
        loadContacts()

        selectSubscribe(PhoneBookViewState::searchTerm) {
            updateState()
        }
    }

    private fun loadContacts() {
        setState {
            copy(
                    mappedContacts = Loading()
            )
        }

        viewModelScope.launch {
            allContacts = contactsDataSource.getContacts()
            mappedContacts = allContacts

            setState {
                copy(
                        mappedContacts = Success(allContacts)
                )
            }

            performLookup(allContacts)
            updateState()
        }
    }

    private fun performLookup(data: List<ContactModel>) {
        viewModelScope.launch {
            val threePids = data.flatMap { contact ->
                contact.emails.map { ThreePid.Email(it.email) } +
                        contact.msisdns.map { ThreePid.Msisdn(it.phoneNumber) }
            }
            session.identityService().lookUp(threePids, object : MatrixCallback<List<FoundThreePid>> {
                override fun onFailure(failure: Throwable) {
                    // Ignore?
                }

                override fun onSuccess(data: List<FoundThreePid>) {
                    foundThreePid = data

                    mappedContacts = allContacts.map { contactModel ->
                        contactModel.copy(
                                emails = contactModel.emails.map { email ->
                                    email.copy(
                                            matrixId = foundThreePid
                                                    .firstOrNull { foundThreePid -> foundThreePid.threePid.value == email.email }
                                                    ?.matrixId
                                    )
                                },
                                msisdns = contactModel.msisdns.map { msisdn ->
                                    msisdn.copy(
                                            matrixId = foundThreePid
                                                    .firstOrNull { foundThreePid -> foundThreePid.threePid.value == msisdn.phoneNumber }
                                                    ?.matrixId
                                    )
                                }
                        )
                    }

                    updateState()
                }
            })
        }
    }

    private fun updateState() = withState { state ->
        val filteredMappedContacts = mappedContacts
                .filter { it.displayName.contains(state.searchTerm, true) }

        setState {
            copy(
                    filteredMappedContacts = filteredMappedContacts
            )
        }
    }

    override fun handle(action: PhoneBookAction) {
        when (action) {
            is PhoneBookAction.FilterWith -> handleFilterWith(action)
        }.exhaustive
    }

    private fun handleFilterWith(action: PhoneBookAction.FilterWith) {
        setState {
            copy(
                    searchTerm = action.filter
            )
        }
    }
}
