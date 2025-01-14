/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
        private val stringProvider: StringProvider,
        private val drawableProvider: DrawableProvider
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

    private val _invites = MutableSharedFlow<InvitesContentState>(replay = 1)
    val invites = _invites.asSharedFlow()

    private var invitesCount = -1

    init {
        observeInvites()
    }

    override fun handle(action: InvitesAction) {
        when (action) {
            is InvitesAction.SelectRoom -> handleSelectRoom(action)
            is InvitesAction.AcceptInvitation -> handleAcceptInvitation(action)
            is InvitesAction.RejectInvitation -> handleRejectInvitation(action)
        }
    }

    private fun handleSelectRoom(action: InvitesAction.SelectRoom) {
        _viewEvents.post(InvitesViewEvents.OpenRoom(
                roomSummary = action.roomSummary,
                shouldCloseInviteView = false,
                isInviteAlreadySelected = false,
        ))
    }

    private fun handleRejectInvitation(action: InvitesAction.RejectInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId
        val roomMembershipChange = state.roomMembershipChanges[roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to left an already leaving or joining room. Should not happen")
            return@withState
        }

        viewModelScope.launch {
            try {
                session.roomService().leaveRoom(roomId)
                // We do not update the rejectingRoomsIds here, because, the room is not rejected yet regarding the sync data.
                // Instead, we wait for the room to be rejected
                // Known bug: if the user is invited again (after rejecting the first invitation), the loading will be displayed instead of the buttons.
                // If we update the state, the button will be displayed again, so it's not ideal...
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

        val shouldCloseInviteView = invitesCount == 1

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

        _viewEvents.post(InvitesViewEvents.OpenRoom(action.roomSummary, shouldCloseInviteView, isInviteAlreadySelected = true))
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

        pagedList.asFlow()
                .map {
                    if (it.isEmpty()) {
                        InvitesContentState.Empty(
                                title = stringProvider.getString(CommonStrings.invites_empty_title),
                                image = drawableProvider.getDrawable(R.drawable.ic_invites_empty),
                                message = stringProvider.getString(CommonStrings.invites_empty_message)
                        )
                    } else {
                        invitesCount = it.loadedCount
                        InvitesContentState.Content(it)
                    }
                }
                .catch {
                    emit(InvitesContentState.Error(it))
                }
                .onStart {
                    emit(InvitesContentState.Loading)
                }.onEach {
                    _invites.emit(it)
                }.launchIn(viewModelScope)
    }
}
