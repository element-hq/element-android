/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.userdirectory.PendingSelection
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom

class InviteUsersToRoomViewModel @AssistedInject constructor(
        @Assisted initialState: InviteUsersToRoomViewState,
        session: Session,
        val stringProvider: StringProvider
) : VectorViewModel<InviteUsersToRoomViewState, InviteUsersToRoomAction, InviteUsersToRoomViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<InviteUsersToRoomViewModel, InviteUsersToRoomViewState> {
        override fun create(initialState: InviteUsersToRoomViewState): InviteUsersToRoomViewModel
    }

    companion object : MavericksViewModelFactory<InviteUsersToRoomViewModel, InviteUsersToRoomViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: InviteUsersToRoomAction) {
        when (action) {
            is InviteUsersToRoomAction.InviteSelectedUsers -> inviteUsersToRoom(action.selections)
        }
    }

    private fun inviteUsersToRoom(selections: Set<PendingSelection>) {
        _viewEvents.post(InviteUsersToRoomViewEvents.Loading)
        selections.asFlow()
                .map { user ->
                    when (user) {
                        is PendingSelection.UserPendingSelection -> room.membershipService().invite(user.user.userId, null)
                        is PendingSelection.ThreePidPendingSelection -> room.membershipService().invite3pid(user.threePid)
                    }
                }.onCompletion { error ->
                    if (error != null) return@onCompletion

                    val successMessage = when (selections.size) {
                        1 -> stringProvider.getString(
                                CommonStrings.invitation_sent_to_one_user,
                                selections.first().getBestName()
                        )
                        2 -> stringProvider.getString(
                                CommonStrings.invitations_sent_to_two_users,
                                selections.first().getBestName(),
                                selections.last().getBestName()
                        )
                        else -> stringProvider.getQuantityString(
                                CommonPlurals.invitations_sent_to_one_and_more_users,
                                selections.size - 1,
                                selections.first().getBestName(),
                                selections.size - 1
                        )
                    }
                    _viewEvents.post(InviteUsersToRoomViewEvents.Success(successMessage))
                }
                .catch { cause ->
                    _viewEvents.post(InviteUsersToRoomViewEvents.Failure(cause))
                }.launchIn(viewModelScope)
    }

    fun getUserIdsOfRoomMembers(): Set<String> {
        return room.roomSummary()?.otherMemberIds?.toSet().orEmpty()
    }
}
