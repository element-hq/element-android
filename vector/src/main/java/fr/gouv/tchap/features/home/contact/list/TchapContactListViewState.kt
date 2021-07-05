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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.contacts.MappedContact
import im.vector.app.features.userdirectory.PendingSelection
import org.matrix.android.sdk.api.session.user.model.User

data class TchapContactListViewState(
        val excludedUserIds: Set<String>? = null,
        val directoryUsers: Async<List<User>> = Uninitialized,

        // All the contacts on the phone
        val mappedContacts: Async<List<MappedContact>> = Loading(),

        // All contacts, filtered by searchTerm
        val filteredLocalUsers: List<User> = emptyList(),

        // True when the identity service has return some data
        val isBoundRetrieved: Boolean = false,
        // The current identity server url if any
        val identityServerUrl: String? = null,
        // User consent to perform lookup (send emails to the identity server)
        val userConsent: Boolean = false,
        // Use to filter contacts by display name
        val searchTerm: String = "",

        // Display search button
        val showSearch: Boolean,
        // Display invite button
        val showInviteActions: Boolean,

        val singleSelection: Boolean,
        val pendingSelections: Set<PendingSelection> = emptySet()
) : MvRxState {
    constructor(args: TchapContactListFragmentArgs) : this(
            excludedUserIds = args.excludedUserIds,
            singleSelection = args.singleSelection,
            showSearch = args.showSearch,
            showInviteActions = args.showInviteActions
    )
}
