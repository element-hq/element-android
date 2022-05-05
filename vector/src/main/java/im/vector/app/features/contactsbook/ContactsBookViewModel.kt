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

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.fetchIdentityServerWithTerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import timber.log.Timber

class ContactsBookViewModel @AssistedInject constructor(
        @Assisted initialState: ContactsBookViewState,
        private val contactsDataSource: ContactsDataSource,
        private val stringProvider: StringProvider,
        private val session: Session
) : VectorViewModel<ContactsBookViewState, ContactsBookAction, ContactsBookViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ContactsBookViewModel, ContactsBookViewState> {
        override fun create(initialState: ContactsBookViewState): ContactsBookViewModel
    }

    companion object : MavericksViewModelFactory<ContactsBookViewModel, ContactsBookViewState> by hiltMavericksViewModelFactory()

    private var allContacts: List<MappedContact> = emptyList()
    private var mappedContacts: List<MappedContact> = emptyList()

    init {
        loadContacts()

        onEach(ContactsBookViewState::searchTerm, ContactsBookViewState::onlyBoundContacts) { _, _ ->
            updateFilteredMappedContacts()
        }
    }

    private fun loadContacts() {
        setState {
            copy(
                    mappedContacts = Loading(),
                    identityServerUrl = session.identityService().getCurrentIdentityServerUrl(),
                    userConsent = session.identityService().getUserConsent()
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

    private fun performLookup(contacts: List<MappedContact>) {
        if (!session.identityService().getUserConsent()) {
            return
        }
        viewModelScope.launch {
            val threePids = contacts.flatMap { contact ->
                contact.emails.map { ThreePid.Email(it.email) } +
                        contact.msisdns.map { ThreePid.Msisdn(it.phoneNumber) }
            }

            val data = try {
                session.identityService().lookUp(threePids)
            } catch (failure: Throwable) {
                Timber.w(failure, "Unable to perform the lookup")

                // Should not happen, but just to be sure
                if (failure is IdentityServiceError.UserConsentNotProvided) {
                    setState {
                        copy(userConsent = false)
                    }
                }
                return@launch
            }

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
    }

    private fun updateFilteredMappedContacts() = withState { state ->
        val filteredMappedContacts = mappedContacts
                .filter { it.displayName.contains(state.searchTerm, true) }
                .filter { contactModel ->
                    !state.onlyBoundContacts ||
                            contactModel.emails.any { it.matrixId != null } || contactModel.msisdns.any { it.matrixId != null }
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
            ContactsBookAction.UserConsentGranted   -> handleUserConsentGranted()
            ContactsBookAction.UserConsentRequest   -> handleUserConsentRequest()
        }
    }

    private fun handleUserConsentRequest() {
        viewModelScope.launch {
            val event = try {
                val result = session.fetchIdentityServerWithTerms(stringProvider.getString(R.string.resources_language))
                ContactsBookViewEvents.OnPoliciesRetrieved(result)
            } catch (throwable: Throwable) {
                ContactsBookViewEvents.Failure(throwable)
            }
            _viewEvents.post(event)
        }
    }

    private fun handleUserConsentGranted() {
        session.identityService().setUserConsent(true)

        setState {
            copy(userConsent = true)
        }

        // Perform the lookup
        performLookup(allContacts)
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
