/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
