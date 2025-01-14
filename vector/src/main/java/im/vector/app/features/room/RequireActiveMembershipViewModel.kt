/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.room

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

/**
 * This ViewModel observe a room summary and notify when the room is left.
 */
class RequireActiveMembershipViewModel @AssistedInject constructor(
        @Assisted initialState: RequireActiveMembershipViewState,
        private val stringProvider: StringProvider,
        private val session: Session
) :
        VectorViewModel<RequireActiveMembershipViewState, RequireActiveMembershipAction, RequireActiveMembershipViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RequireActiveMembershipViewModel, RequireActiveMembershipViewState> {
        override fun create(initialState: RequireActiveMembershipViewState): RequireActiveMembershipViewModel
    }

    companion object : MavericksViewModelFactory<RequireActiveMembershipViewModel, RequireActiveMembershipViewState> by hiltMavericksViewModelFactory()

    private val roomIdFlow = MutableStateFlow(Optional.from(initialState.roomId))

    init {
        observeRoomSummary()
    }

    private fun observeRoomSummary() {
        roomIdFlow
                .unwrap()
                .flatMapLatest { roomId ->
                    val room = session.getRoom(roomId) ?: return@flatMapLatest flow {
                        val emptyResult = Optional.empty<RequireActiveMembershipViewEvents.RoomLeft>()
                        emit(emptyResult)
                    }
                    room.flow()
                            .liveRoomSummary()
                            .unwrap()
                            .map { mapToLeftViewEvent(room, it) }
                            .flowOn(Dispatchers.Default)
                }
                .unwrap()
                .onEach { event ->
                    _viewEvents.post(event)
                }
                .launchIn(viewModelScope)
    }

    private fun mapToLeftViewEvent(room: Room, roomSummary: RoomSummary): Optional<RequireActiveMembershipViewEvents.RoomLeft> {
        if (roomSummary.membership.isActive()) {
            return Optional.empty()
        }
        val senderId = room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(session.myUserId))?.senderId
        val senderDisplayName = senderId?.takeIf { it != session.myUserId }?.let {
            room.membershipService().getRoomMember(it)?.displayName ?: it
        }
        val viewEvent = when (roomSummary.membership) {
            Membership.LEAVE -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(CommonStrings.has_been_removed, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            Membership.KNOCK -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(CommonStrings.has_been_removed, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            Membership.BAN -> {
                val message = senderDisplayName?.let {
                    stringProvider.getString(CommonStrings.has_been_banned, roomSummary.displayName, it)
                }
                RequireActiveMembershipViewEvents.RoomLeft(message)
            }
            else -> null
        }
        return Optional.from(viewEvent)
    }

    override fun handle(action: RequireActiveMembershipAction) {
        when (action) {
            is RequireActiveMembershipAction.ChangeRoom -> {
                setState {
                    copy(roomId = action.roomId)
                }
                roomIdFlow.tryEmit(Optional.from(action.roomId))
            }
        }
    }
}
