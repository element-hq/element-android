/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.invite

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.peeking.PeekResult

class SpaceInviteBottomSheetViewModel @AssistedInject constructor(
        @Assisted private val initialState: SpaceInviteBottomSheetState,
        private val session: Session,
        private val errorFormatter: ErrorFormatter
) : VectorViewModel<SpaceInviteBottomSheetState, SpaceInviteBottomSheetAction, SpaceInviteBottomSheetEvents>(initialState) {

    init {
        session.getRoomSummary(initialState.spaceId)?.let { roomSummary ->
            val knownMembers = roomSummary.otherMemberIds.filter {
                session.roomService().getExistingDirectRoomWithUser(it) != null
            }.map { session.getUserOrDefault(it) }
            // put one with avatar first, and take 5
            val peopleYouKnow = (knownMembers.filter { it.avatarUrl != null } + knownMembers.filter { it.avatarUrl == null })
                    .take(5)

            setState {
                copy(
                        summary = Success(roomSummary),
                        inviterUser = roomSummary.inviterId?.let { session.getUserOrDefault(it) }?.let { Success(it) } ?: Uninitialized,
                        peopleYouKnow = Success(peopleYouKnow)
                )
            }
            if (roomSummary.membership == Membership.INVITE) {
                getLatestRoomSummary(roomSummary)
            }
        }
    }

    /**
     * Try to request the room summary api to get more info.
     */
    private fun getLatestRoomSummary(roomSummary: RoomSummary) {
        viewModelScope.launch(Dispatchers.IO) {
            val peekResult = tryOrNull { session.roomService().peekRoom(roomSummary.roomId) } as? PeekResult.Success ?: return@launch
            setState {
                copy(
                        summary = Success(
                                roomSummary.copy(
                                        joinedMembersCount = peekResult.numJoinedMembers,
                                        // it's also possible that the name/avatar did change since the invite..
                                        // if it's null keep the old one as summary API might not be available
                                        // and peek result could be null for other reasons (not peekable)
                                        avatarUrl = peekResult.avatarUrl ?: roomSummary.avatarUrl,
                                        displayName = peekResult.name ?: roomSummary.displayName,
                                        topic = peekResult.topic ?: roomSummary.topic
                                        // maybe use someMembers field later?
                                )
                        )
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpaceInviteBottomSheetViewModel, SpaceInviteBottomSheetState> {
        override fun create(initialState: SpaceInviteBottomSheetState): SpaceInviteBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<SpaceInviteBottomSheetViewModel, SpaceInviteBottomSheetState> by hiltMavericksViewModelFactory()

    override fun handle(action: SpaceInviteBottomSheetAction) {
        when (action) {
            SpaceInviteBottomSheetAction.DoJoin -> {
                setState { copy(joinActionState = Loading()) }
                session.coroutineScope.launch(Dispatchers.IO) {
                    try {
                        session.spaceService().joinSpace(initialState.spaceId)
                        setState { copy(joinActionState = Success(Unit)) }
                    } catch (failure: Throwable) {
                        setState { copy(joinActionState = Fail(failure)) }
                        _viewEvents.post(SpaceInviteBottomSheetEvents.ShowError(errorFormatter.toHumanReadable(failure)))
                    }
                }
            }
            SpaceInviteBottomSheetAction.DoReject -> {
                setState { copy(rejectActionState = Loading()) }
                session.coroutineScope.launch(Dispatchers.IO) {
                    try {
                        session.spaceService().leaveSpace(initialState.spaceId)
                        setState { copy(rejectActionState = Success(Unit)) }
                    } catch (failure: Throwable) {
                        setState { copy(rejectActionState = Fail(failure)) }
                        _viewEvents.post(SpaceInviteBottomSheetEvents.ShowError(errorFormatter.toHumanReadable(failure)))
                    }
                }
            }
        }
    }
}
