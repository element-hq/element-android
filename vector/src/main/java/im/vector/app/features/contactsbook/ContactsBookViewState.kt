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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import im.vector.app.core.contacts.MappedContact

data class ContactsBookViewState(
        // All the contacts on the phone
        val mappedContacts: Async<List<MappedContact>> = Loading(),
        // Use to filter contacts by display name
        val searchTerm: String = "",
        // True to display only bound contacts with their bound 2pid
        val onlyBoundContacts: Boolean = false,
        // All contacts, filtered by searchTerm and onlyBoundContacts
        val filteredMappedContacts: List<MappedContact> = emptyList(),
        // True when the identity service has return some data
        val isBoundRetrieved: Boolean = false,
        // The current identity server url if any
        val identityServerUrl: String? = null,
        // User consent to perform lookup (send emails to the identity server)
        val userConsent: Boolean = false
) : MavericksState
