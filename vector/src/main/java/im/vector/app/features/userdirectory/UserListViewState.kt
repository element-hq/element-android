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

import androidx.paging.PagedList
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.contacts.MappedContact
import org.matrix.android.sdk.api.session.user.model.User

data class UserListViewState(
        val excludedUserIds: Set<String>? = null,
        val knownUsers: Async<PagedList<User>> = Uninitialized,
        val directoryUsers: Async<List<User>> = Uninitialized,
        val matchingEmail: Async<ThreePidUser?> = Uninitialized,
        val filteredMappedContacts: List<MappedContact> = emptyList(),
        val pendingSelections: Set<PendingSelection> = emptySet(),
        val searchTerm: String = "",
        val singleSelection: Boolean,
        val configuredIdentityServer: String? = null,
        private val showInviteActions: Boolean,
        val showContactBookAction: Boolean
) : MavericksState {

    constructor(args: UserListFragmentArgs) : this(
            excludedUserIds = args.excludedUserIds,
            singleSelection = args.singleSelection,
            showInviteActions = args.showInviteActions,
            showContactBookAction = args.showContactBookAction
    )

    fun getSelectedMatrixId(): List<String> {
        return pendingSelections
                .mapNotNull {
                    when (it) {
                        is PendingSelection.UserPendingSelection     -> it.user.userId
                        is PendingSelection.ThreePidPendingSelection -> null
                    }
                }
    }

    fun showInviteActions() = showInviteActions && pendingSelections.isEmpty()
}
