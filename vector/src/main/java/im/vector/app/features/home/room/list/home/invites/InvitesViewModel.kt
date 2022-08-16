/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home.invites

import androidx.paging.PagedList
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import timber.log.Timber

class InvitesViewModel @AssistedInject constructor(
        @Assisted val initialState: InvitesViewState,
        private val session: Session,
) : VectorViewModel<InvitesViewState, InvitesAction, InvitesViewEvents>(initialState) {

    private val pagedListConfig = PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(20)
            .setEnablePlaceholders(true)
            .setPrefetchDistance(10)
            .build()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<InvitesViewModel, InvitesViewState> {
        override fun create(initialState: InvitesViewState): InvitesViewModel
    }

    companion object : MavericksViewModelFactory<InvitesViewModel, InvitesViewState> by hiltMavericksViewModelFactory()

    init {
        observeInvites()
    }

    override fun handle(action: InvitesAction) {
        when (action) {
            is InvitesAction.AcceptInvitation -> handleAcceptInvitation(action)
            is InvitesAction.RejectInvitation -> handleRejectInvitation(action)
        }
    }

    private fun handleRejectInvitation(action: InvitesAction.RejectInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId
        val roomMembershipChange = state.roomMembershipChanges[roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to left an already leaving or joining room. Should not happen")
            return@withState
        }

        val shouldCloseInviteView = state.pagedList?.value?.size == 1

        viewModelScope.launch {
            try {
                session.roomService().leaveRoom(roomId)
                // We do not update the rejectingRoomsIds here, because, the room is not rejected yet regarding the sync data.
                // Instead, we wait for the room to be rejected
                // Known bug: if the user is invited again (after rejecting the first invitation), the loading will be displayed instead of the buttons.
                // If we update the state, the button will be displayed again, so it's not ideal...
                if (shouldCloseInviteView) {
                    _viewEvents.post(InvitesViewEvents.Close)
                }
            } catch (failure: Throwable) {
                // Notify the user
                _viewEvents.post(InvitesViewEvents.Failure(failure))
            }
        }
    }

    private fun handleAcceptInvitation(action: InvitesAction.AcceptInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId
        val roomMembershipChange = state.roomMembershipChanges[roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }
        // close invites view when navigate to a room from the last one invite

        val shouldCloseInviteView = state.pagedList?.value?.size == 1

        _viewEvents.post(InvitesViewEvents.OpenRoom(action.roomSummary, shouldCloseInviteView))

        // quick echo
        setState {
            copy(
                    roomMembershipChanges = roomMembershipChanges.mapValues {
                        if (it.key == roomId) {
                            ChangeMembershipState.Joining
                        } else {
                            it.value
                        }
                    }
            )
        }
    }

    private fun observeInvites() {
        val builder = RoomSummaryQueryParams.Builder().also {
            it.memberships = listOf(Membership.INVITE)
        }
        val pagedList = session.roomService().getPagedRoomSummariesLive(
                queryParams = builder.build(),
                pagedListConfig = pagedListConfig,
                sortOrder = RoomSortOrder.ACTIVITY
        )

        setState {
            copy(pagedList = pagedList)
        }
    }
}
