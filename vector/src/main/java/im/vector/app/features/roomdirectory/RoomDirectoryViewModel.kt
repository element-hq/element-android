/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.appendAt
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.analytics.plan.JoinedRoom
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsFilter
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

class RoomDirectoryViewModel @AssistedInject constructor(
        @Assisted initialState: PublicRoomsViewState,
        vectorPreferences: VectorPreferences,
        private val session: Session,
        private val analyticsTracker: AnalyticsTracker,
        private val explicitTermFilter: ExplicitTermFilter
) : VectorViewModel<PublicRoomsViewState, RoomDirectoryAction, RoomDirectoryViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> {
        override fun create(initialState: PublicRoomsViewState): RoomDirectoryViewModel
    }

    companion object : MavericksViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> by hiltMavericksViewModelFactory() {
        private const val PUBLIC_ROOMS_LIMIT = 20
    }

    private val showAllRooms = vectorPreferences.showAllPublicRooms()

    private var since: String? = null

    private var currentJob: Job? = null

    init {
        // Observe joined room (from the sync)
        observeJoinedRooms()
        observeMembershipChanges()
    }

    private fun observeJoinedRooms() {
        val queryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
        }
        session
                .flow()
                .liveRoomSummaries(queryParams)
                .map { roomSummaries ->
                    roomSummaries
                            .map { it.roomId }
                            .toSet()
                }
                .setOnEach {
                    copy(joinedRoomsIds = it)
                }
    }

    private fun observeMembershipChanges() {
        session.flow()
                .liveRoomChangeMembershipState()
                .setOnEach {
                    copy(changeMembershipStates = it)
                }
    }

    override fun handle(action: RoomDirectoryAction) {
        when (action) {
            is RoomDirectoryAction.SetRoomDirectoryData -> setRoomDirectoryData(action)
            is RoomDirectoryAction.FilterWith -> filterWith(action)
            RoomDirectoryAction.LoadMore -> loadMore()
            is RoomDirectoryAction.JoinRoom -> joinRoom(action)
        }
    }

    private fun setRoomDirectoryData(action: RoomDirectoryAction.SetRoomDirectoryData) = withState {
        if (it.roomDirectoryData == action.roomDirectoryData) {
            return@withState
        }
        setState {
            copy(roomDirectoryData = action.roomDirectoryData)
        }
        reset("")
        load("", action.roomDirectoryData)
    }

    private fun filterWith(action: RoomDirectoryAction.FilterWith) = withState { state ->
        if (state.currentFilter != action.filter) {
            currentJob?.cancel()

            reset(action.filter)
            load(action.filter, state.roomDirectoryData)
        }
    }

    private fun reset(newFilter: String) {
        // Reset since token
        since = null

        setState {
            copy(
                    publicRooms = emptyList(),
                    asyncPublicRoomsRequest = Loading(),
                    hasMore = false,
                    currentFilter = newFilter
            )
        }
    }

    private fun loadMore() = withState { state ->
        if (currentJob == null) {
            setState {
                copy(
                        asyncPublicRoomsRequest = Loading()
                )
            }
            load(state.currentFilter, state.roomDirectoryData)
        }
    }

    private fun load(filter: String, roomDirectoryData: RoomDirectoryData) {
        if (!showAllRooms && !explicitTermFilter.canSearchFor(filter)) {
            setState {
                copy(
                        asyncPublicRoomsRequest = Success(Unit),
                        publicRooms = emptyList(),
                        hasMore = false
                )
            }
            return
        }

        currentJob = viewModelScope.launch {
            val data = try {
                session.roomDirectoryService().getPublicRooms(
                        roomDirectoryData.homeServer,
                        PublicRoomsParams(
                                limit = PUBLIC_ROOMS_LIMIT,
                                filter = PublicRoomsFilter(searchTerm = filter),
                                includeAllNetworks = roomDirectoryData.includeAllNetworks,
                                since = since,
                                thirdPartyInstanceId = roomDirectoryData.thirdPartyInstanceId
                        )
                )
            } catch (failure: Throwable) {
                if (failure is CancellationException) {
                    // Ignore, another request should be already started
                    return@launch
                }

                currentJob = null

                setState {
                    copy(
                            asyncPublicRoomsRequest = Fail(failure)
                    )
                }
                null
            }

            data ?: return@launch

            currentJob = null

            since = data.nextBatch

            // Filter
            val newPublicRooms = data.chunk.orEmpty()
                    .filter {
                        showAllRooms || explicitTermFilter.isValid("${it.name.orEmpty()} ${it.topic.orEmpty()}")
                    }

            setState {
                copy(
                        asyncPublicRoomsRequest = Success(Unit),
                        // It's ok to append at the end of the list, so I use publicRooms.size()
                        publicRooms = publicRooms.appendAt(newPublicRooms, publicRooms.size)
                                // Rageshake #8206 tells that we can have several times the same room
                                .distinctBy { it.roomId },
                        hasMore = since != null
                )
            }
        }
    }

    private fun joinRoom(action: RoomDirectoryAction.JoinRoom) = withState { state ->
        val roomMembershipChange = state.changeMembershipStates[action.publicRoom.roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }
        val viaServers = listOfNotNull(state.roomDirectoryData.homeServer)
        viewModelScope.launch {
            try {
                session.roomService().joinRoom(action.publicRoom.roomId, viaServers = viaServers)
                analyticsTracker.capture(action.publicRoom.toAnalyticsJoinedRoom(JoinedRoom.Trigger.RoomDirectory))
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            } catch (failure: Throwable) {
                // Notify the user
                _viewEvents.post(RoomDirectoryViewEvents.Failure(failure))
            }
        }
    }
}
