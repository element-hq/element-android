/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        val unknownUserId: String? = null,
        val searchTerm: String = "",
        val singleSelection: Boolean,
        val single3pidSelection: Boolean,
        val isE2EByDefault: Boolean = false,
        val configuredIdentityServer: String? = null,
        private val showInviteActions: Boolean,
        val showContactBookAction: Boolean
) : MavericksState {

    constructor(args: UserListFragmentArgs) : this(
            excludedUserIds = args.excludedUserIds,
            singleSelection = args.singleSelection,
            single3pidSelection = args.single3pidSelection,
            showInviteActions = args.showInviteActions,
            showContactBookAction = args.showContactBookAction
    )

    fun getSelectedMatrixId(): List<String> {
        return pendingSelections
                .mapNotNull {
                    when (it) {
                        is PendingSelection.UserPendingSelection -> it.user.userId
                        is PendingSelection.ThreePidPendingSelection -> null
                    }
                }
    }

    fun showInviteActions() = showInviteActions && pendingSelections.isEmpty()
}
