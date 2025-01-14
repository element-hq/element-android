/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.roompreview

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.analytics.plan.JoinedRoom
import im.vector.app.features.roomdirectory.JoinState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber

class RoomPreviewViewModel @AssistedInject constructor(
        @Assisted private val initialState: RoomPreviewViewState,
        private val analyticsTracker: AnalyticsTracker,
        private val session: Session
) : VectorViewModel<RoomPreviewViewState, RoomPreviewAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomPreviewViewModel, RoomPreviewViewState> {
        override fun create(initialState: RoomPreviewViewState): RoomPreviewViewModel
    }

    companion object : MavericksViewModelFactory<RoomPreviewViewModel, RoomPreviewViewState> by hiltMavericksViewModelFactory()

    init {
        // Observe joined room (from the sync)
        observeRoomSummary()
        observeMembershipChanges()

        if (initialState.fromEmailInvite != null) {
            setState {
                copy(peekingState = Loading())
            }
            viewModelScope.launch {
                // we might want to check if the mail is bound to this account?
                // if it is the invite
                val threePids = session
                        .profileService()
                        .getThreePids()
                        .filterIsInstance<ThreePid.Email>()

                val status = if (threePids.any { it.email == initialState.fromEmailInvite.email }) {
                    try {
                        session.identityService().getShareStatus(threePids)
                    } catch (failure: Throwable) {
                        Timber.w(failure, "## Room Invite: Failed to get 3pids shared status")
                        // If terms not signed, or no identity server setup, or different
                        // id server from the one in the email invite, we consider the mails as not bound
                        emptyMap()
                    }.firstNotNullOfOrNull { if (it.key.value == initialState.fromEmailInvite.email) it.value else null }
                            ?: SharedState.NOT_SHARED
                } else {
                    SharedState.NOT_SHARED
                }

                setState {
                    copy(
                            isEmailBoundToAccount = status == SharedState.SHARED,
                            peekingState = Success(PeekingState.FOUND)
                    )
                }
            }
        } else if (initialState.shouldPeekFromServer) {
            peekRoomFromServer()
        }
    }

    private fun peekRoomFromServer() {
        setState {
            copy(peekingState = Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            val peekResult = tryOrNull {
                session.roomService().peekRoom(initialState.roomAlias ?: initialState.roomId)
            }

            when (peekResult) {
                is PeekResult.Success -> {
                    setState {
                        // Do not override what we had from the permalink
                        val newHomeServers = if (homeServers.isEmpty()) {
                            peekResult.viaServers.take(3)
                        } else {
                            homeServers
                        }
                        copy(
                                roomId = peekResult.roomId,
                                avatarUrl = peekResult.avatarUrl,
                                roomAlias = peekResult.alias ?: initialState.roomAlias,
                                roomTopic = peekResult.topic,
                                homeServers = newHomeServers,
                                peekingState = Success(PeekingState.FOUND)
                        )
                    }
                }
                is PeekResult.PeekingNotAllowed -> {
                    setState {
                        // Do not override what we had from the permalink
                        val newHomeServers = if (homeServers.isEmpty()) {
                            peekResult.viaServers.take(3)
                        } else {
                            homeServers
                        }
                        copy(
                                roomId = peekResult.roomId,
                                roomAlias = peekResult.alias ?: initialState.roomAlias,
                                homeServers = newHomeServers,
                                peekingState = Success(PeekingState.NO_ACCESS)
                        )
                    }
                }
                PeekResult.UnknownAlias,
                null -> {
                    setState {
                        copy(
                                peekingState = Success(PeekingState.NOT_FOUND)
                        )
                    }
                }
            }
        }
    }

    private fun observeRoomSummary() {
        session
                .flow()
                .liveRoomSummary(initialState.roomId)
                .unwrap()
                .onEach { roomSummary ->
                    val isRoomJoined = roomSummary.membership == Membership.JOIN
                    if (isRoomJoined) {
                        setState {
                            copy(
                                    roomType = roomSummary.roomType,
                                    roomJoinState = JoinState.JOINED
                            )
                        }
                    } else {
                        setState {
                            copy(
                                    roomType = roomSummary.roomType
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun observeMembershipChanges() {
        session.flow()
                .liveRoomChangeMembershipState()
                .onEach {
                    val changeMembership = it[initialState.roomId] ?: ChangeMembershipState.Unknown
                    val joinState = when (changeMembership) {
                        is ChangeMembershipState.Joining -> JoinState.JOINING
                        is ChangeMembershipState.FailedJoining -> JoinState.JOINING_ERROR
                        // Other cases are handled by room summary
                        else -> null
                    }
                    if (joinState != null) {
                        setState { copy(roomJoinState = joinState) }
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: RoomPreviewAction) {
        when (action) {
            is RoomPreviewAction.Join -> handleJoinRoom()
            RoomPreviewAction.JoinThirdParty -> handleJoinRoomThirdParty()
        }
    }

    private fun handleJoinRoomThirdParty() = withState { state ->
        if (state.roomJoinState == JoinState.JOINING) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }
        // local echo
        setState {
            copy(roomJoinState = JoinState.JOINING)
        }
        viewModelScope.launch {
            try {
                // XXX this could be done locally, but the spec is incomplete and it's not clear
                // what needs to be signed with what?
                val thirdPartySigned = session.identityService().sign3pidInvitation(
                        state.fromEmailInvite?.identityServer ?: "",
                        state.fromEmailInvite?.token ?: "",
                        state.fromEmailInvite?.privateKey ?: ""
                )

                session.roomService().joinRoom(state.roomId, reason = null, thirdPartySigned)
            } catch (failure: Throwable) {
                setState {
                    copy(
                            roomJoinState = JoinState.JOINING_ERROR,
                            lastError = failure
                    )
                }
            }
        }
    }

    private fun handleJoinRoom() = withState { state ->
        if (state.roomJoinState == JoinState.JOINING) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }
        viewModelScope.launch {
            try {
                session.roomService().joinRoom(state.roomId, viaServers = state.homeServers)
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            } catch (failure: Throwable) {
                setState { copy(lastError = failure) }
            }
            session.getRoomSummary(state.roomId)
                    ?.let { analyticsTracker.capture(it.toAnalyticsJoinedRoom(JoinedRoom.Trigger.RoomPreview)) }
        }
    }
}
