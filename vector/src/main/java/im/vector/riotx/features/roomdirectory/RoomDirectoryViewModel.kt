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

package im.vector.riotx.features.roomdirectory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsFilter
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsParams
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsResponse
import im.vector.matrix.android.api.session.room.model.thirdparty.RoomDirectoryData
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent
import timber.log.Timber

private const val PUBLIC_ROOMS_LIMIT = 20

class RoomDirectoryViewModel @AssistedInject constructor(@Assisted initialState: PublicRoomsViewState,
                                                         private val session: Session) : VectorViewModel<PublicRoomsViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: PublicRoomsViewState): RoomDirectoryViewModel
    }

    companion object : MvRxViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: PublicRoomsViewState): RoomDirectoryViewModel? {
            val activity: RoomDirectoryActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.roomDirectoryViewModelFactory.create(state)
        }
    }

    private val _joinRoomErrorLiveData = MutableLiveData<LiveEvent<Throwable>>()
    val joinRoomErrorLiveData: LiveData<LiveEvent<Throwable>>
        get() = _joinRoomErrorLiveData


    private var since: String? = null

    private var currentTask: Cancelable? = null

    // Default RoomDirectoryData
    private var roomDirectoryData = RoomDirectoryData()

    init {
        setState {
            copy(
                    roomDirectoryDisplayName = roomDirectoryData.displayName
            )
        }

        // Observe joined room (from the sync)
        observeJoinedRooms()
    }

    private fun observeJoinedRooms() {
        session
                .rx()
                .liveRoomSummaries()
                .subscribe { list ->
                    val joinedRoomIds = list
                            // Keep only joined room
                            ?.filter { it.membership == Membership.JOIN }
                            ?.map { it.roomId }
                            ?.toSet()
                            ?: emptySet()

                    setState {
                        copy(
                                joinedRoomsIds = joinedRoomIds,
                                // Remove (newly) joined room id from the joining room list
                                joiningRoomsIds = joiningRoomsIds.toMutableSet().apply { removeAll(joinedRoomIds) },
                                // Remove (newly) joined room id from the joining room list in error
                                joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableSet().apply { removeAll(joinedRoomIds) }
                        )
                    }
                }
                .disposeOnClear()
    }

    fun setRoomDirectoryData(roomDirectoryData: RoomDirectoryData) {
        if (this.roomDirectoryData == roomDirectoryData) {
            return
        }

        this.roomDirectoryData = roomDirectoryData

        reset("")
        load("")
    }

    fun filterWith(filter: String) = withState { state ->
        if (state.currentFilter != filter) {
            currentTask?.cancel()

            reset(filter)
            load(filter)
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
                    roomDirectoryDisplayName = roomDirectoryData.displayName,
                    currentFilter = newFilter
            )
        }
    }

    fun loadMore() = withState { state ->
        if (currentTask == null) {
            setState {
                copy(
                        asyncPublicRoomsRequest = Loading()
                )
            }

            load(state.currentFilter)
        }
    }

    private fun load(filter: String) {
        currentTask = session.getPublicRooms(roomDirectoryData.homeServer,
                PublicRoomsParams(
                        limit = PUBLIC_ROOMS_LIMIT,
                        filter = PublicRoomsFilter(searchTerm = filter),
                        includeAllNetworks = roomDirectoryData.includeAllNetworks,
                        since = since,
                        thirdPartyInstanceId = roomDirectoryData.thirdPartyInstanceId
                ),
                object : MatrixCallback<PublicRoomsResponse> {
                    override fun onSuccess(data: PublicRoomsResponse) {
                        currentTask = null

                        since = data.nextBatch

                        setState {
                            copy(
                                    asyncPublicRoomsRequest = Success(data.chunk!!),
                                    // It's ok to append at the end of the list, so I use publicRooms.size()
                                    publicRooms = publicRooms.appendAt(data.chunk!!, publicRooms.size),
                                    hasMore = since != null
                            )
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        currentTask = null

                        setState {
                            copy(
                                    asyncPublicRoomsRequest = Fail(failure)
                            )
                        }
                    }
                })
    }

    fun joinRoom(publicRoom: PublicRoom) = withState { state ->
        if (state.joiningRoomsIds.contains(publicRoom.roomId)) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }

        setState {
            copy(
                    joiningRoomsIds = joiningRoomsIds.toMutableSet().apply { add(publicRoom.roomId) }
            )
        }

        session.joinRoom(publicRoom.roomId, emptyList(), object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _joinRoomErrorLiveData.postValue(LiveEvent(failure))

                setState {
                    copy(
                            joiningRoomsIds = joiningRoomsIds.toMutableSet().apply { remove(publicRoom.roomId) },
                            joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableSet().apply { add(publicRoom.roomId) }
                    )
                }
            }
        })
    }

}