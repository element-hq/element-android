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

package im.vector.app.features.roomdirectory

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.roomdirectory.picker.RoomDirectoryListCreator
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsFilter
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow

class RoomDirectoryViewModel @AssistedInject constructor(
        @Assisted initialState: PublicRoomsViewState,
        vectorPreferences: VectorPreferences,
        private val session: Session,
        private val explicitTermFilter: ExplicitTermFilter,
        private val roomDirectoryListCreator: RoomDirectoryListCreator
) : VectorViewModel<PublicRoomsViewState, RoomDirectoryAction, RoomDirectoryViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> {
        override fun create(initialState: PublicRoomsViewState): RoomDirectoryViewModel
    }

    companion object : MavericksViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> by hiltMavericksViewModelFactory() {
        private const val PUBLIC_ROOMS_LIMIT = 20
    }

    private val showAllRooms = vectorPreferences.showAllPublicRooms()

    private var currentJob: Job? = null

    init {
        // Observe joined room (from the sync)
        observeAndCompute()
        load()
        observeJoinedRooms()
        observeMembershipChanges()
    }

    private fun observeAndCompute() {
        onEach(
                PublicRoomsViewState::asyncThirdPartyRequest
        ) { async ->
            async()?.let {
                setState {
                    copy(directories = roomDirectoryListCreator.computeDirectories(it, emptySet()))
                }
                loadMore()
            }
        }
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
            is RoomDirectoryAction.SetRoomDirectoryData -> Unit
            is RoomDirectoryAction.FilterWith           -> filterWith(action)
            is RoomDirectoryAction.JoinRoom             -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState {
                copy(asyncThirdPartyRequest = Loading())
            }
            try {
                val thirdPartyProtocols = session.thirdPartyService().getThirdPartyProtocols()
                setState {
                    copy(asyncThirdPartyRequest = Success(thirdPartyProtocols))
                }
            } catch (failure: Throwable) {
                setState {
                    copy(asyncThirdPartyRequest = Fail(failure))
                }
            }
        }
    }

    private fun filterWith(action: RoomDirectoryAction.FilterWith) = withState { state ->
        if (state.currentFilter != action.filter) {
            currentJob?.cancel()

            reset(action.filter)
            load(action.filter, state.directories)
        }
    }

    private fun reset(newFilter: String) {
        setState {
            copy(
                    publicRooms = emptyMap(),
                    asyncPublicRoomsRequest = Loading(),
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
            load(state.currentFilter, state.directories)
        }
    }

    private fun load(filter: String, roomDirectories: List<RoomDirectoryServer>) {
        if (!showAllRooms && !explicitTermFilter.canSearchFor(filter)) {
            setState {
                copy(
                        asyncPublicRoomsRequest = Success(Unit),
                        publicRooms = emptyMap()
                )
            }
            return
        }

        val newPublicRooms = mutableMapOf<PublicRoom, RoomDirectoryData>()
        val mutex = Mutex()

        currentJob = viewModelScope.launch {
            roomDirectories.map { roomDirectoryServer ->
                val roomDirectoryData = roomDirectoryServer.protocols.first()
                async {
                    val data = try {
                        session.getPublicRooms(roomDirectoryData.homeServer,
                                PublicRoomsParams(
                                        filter = PublicRoomsFilter(searchTerm = filter),
                                        includeAllNetworks = roomDirectoryData.includeAllNetworks,
                                        thirdPartyInstanceId = roomDirectoryData.thirdPartyInstanceId
                                )
                        )
                    } catch (failure: Throwable) {
                        if (failure is CancellationException) {
                            // Ignore, another request should be already started
                            return@async
                        }

                        setState {
                            copy(
                                    asyncPublicRoomsRequest = Fail(failure)
                            )
                        }
                        null
                    }

                    data ?: return@async

                    // Filter
                    mutex.withLock {
                        newPublicRooms.putAll(data.chunk.orEmpty()
                                .filter {
                                    showAllRooms || explicitTermFilter.isValid("${it.name.orEmpty()} ${it.topic.orEmpty()}")
                                }.map { it to roomDirectoryData }.toMap())
                    }
                }
            }.joinAll()

            currentJob = null

            setState {
                copy(
                        asyncPublicRoomsRequest = Success(Unit),
                        publicRooms = newPublicRooms
                )
            }
        }
    }

//    private fun joinRoom(action: RoomDirectoryAction.JoinRoom) = withState { state ->
//        val roomMembershipChange = state.changeMembershipStates[action.roomId]
//        if (roomMembershipChange?.isInProgress().orFalse()) {
//            // Request already sent, should not happen
//            Timber.w("Try to join an already joining room. Should not happen")
//            return@withState
//        }
//        val viaServers = listOfNotNull(state.roomDirectoryData.homeServer)
//        viewModelScope.launch {
//            try {
//                session.joinRoom(action.roomId, viaServers = viaServers)
//                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
//                // Instead, we wait for the room to be joined
//            } catch (failure: Throwable) {
//                // Notify the user
//                _viewEvents.post(RoomDirectoryViewEvents.Failure(failure))
//            }
//        }
//    }

    override fun onCleared() {
        currentJob?.cancel()
        super.onCleared()
    }
}
