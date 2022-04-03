/*
 * Copyright 2019 New Vector Ltd
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
import im.vector.app.features.analytics.extensions.toAnalyticsRoomSize
import im.vector.app.features.analytics.plan.JoinedRoom
import im.vector.app.features.roomdirectory.JoinState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow
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
                session.peekRoom(initialState.roomAlias ?: initialState.roomId)
            }

            when (peekResult) {
                is PeekResult.Success           -> {
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
                null                            -> {
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
        val queryParams = roomSummaryQueryParams {
            roomId = QueryStringValue.Equals(initialState.roomId)
            excludeType = null
        }
        session
                .flow()
                .liveRoomSummaries(queryParams)
                .onEach { list ->
                    val isRoomJoined = list.any {
                        it.membership == Membership.JOIN
                    }
                    list.firstOrNull { it.roomId == initialState.roomId }?.roomType?.let {
                        setState {
                            copy(roomType = it)
                        }
                    }
                    if (isRoomJoined) {
                        setState { copy(roomJoinState = JoinState.JOINED) }
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
                        is ChangeMembershipState.Joining       -> JoinState.JOINING
                        is ChangeMembershipState.FailedJoining -> JoinState.JOINING_ERROR
                        // Other cases are handled by room summary
                        else                                   -> null
                    }
                    if (joinState != null) {
                        setState { copy(roomJoinState = joinState) }
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: RoomPreviewAction) {
        when (action) {
            is RoomPreviewAction.Join        -> handleJoinRoom()
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

                session.joinRoom(state.roomId, reason = null, thirdPartySigned)
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
                session.joinRoom(state.roomId, viaServers = state.homeServers)
                analyticsTracker.capture(JoinedRoom(
                        // Always false in this case (?)
                        isDM = false,
                        isSpace = false,
                        roomSize = state.numJoinMembers.toAnalyticsRoomSize(),
                ))
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            } catch (failure: Throwable) {
                setState { copy(lastError = failure) }
            }
        }
    }
}
