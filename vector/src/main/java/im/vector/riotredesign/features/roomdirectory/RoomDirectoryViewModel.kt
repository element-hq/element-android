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

package im.vector.riotredesign.features.roomdirectory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.*
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
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import org.koin.android.ext.android.get
import timber.log.Timber

private const val PUBLIC_ROOMS_LIMIT = 20

class RoomDirectoryViewModel(initialState: PublicRoomsViewState,
                             private val session: Session) : VectorViewModel<PublicRoomsViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomDirectoryViewModel, PublicRoomsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: PublicRoomsViewState): RoomDirectoryViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()

            return RoomDirectoryViewModel(state, currentSession)
        }
    }

    private val _joinRoomErrorLiveData = MutableLiveData<LiveEvent<Throwable>>()
    val joinRoomErrorLiveData: LiveData<LiveEvent<Throwable>>
        get() = _joinRoomErrorLiveData


    // TODO Store in ViewState?
    private var currentFilter: String = ""

    private var since: String? = null

    private var currentTask: Cancelable? = null

    // Default RoomDirectoryData
    private var roomDirectoryData = RoomDirectoryData()

    init {
        // Load with empty filter
        load()

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
                .execute { async ->
                    val joinedRoomIds = async.invoke()
                            // Keep only joined room
                            ?.filter { it.membership == Membership.JOIN }
                            ?.map { it.roomId }
                            ?.toList()
                            ?: emptyList()

                    copy(
                            joinedRoomsIds = joinedRoomIds,
                            // Remove (newly) joined room id from the joining room list
                            joiningRoomsIds = joiningRoomsIds.toMutableList().apply { removeAll(joinedRoomIds) },
                            // Remove (newly) joined room id from the joining room list in error
                            joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableList().apply { removeAll(joinedRoomIds) }
                    )
                }
    }

    fun setRoomDirectoryData(roomDirectoryData: RoomDirectoryData) {
        if (this.roomDirectoryData == roomDirectoryData) {
            return
        }

        this.roomDirectoryData = roomDirectoryData

        reset()
        load()
    }

    fun filterWith(filter: String) {
        if (currentFilter == filter) {
            return
        }

        currentTask?.cancel()

        currentFilter = filter

        reset()
        load()
    }

    private fun reset() {
        // Reset since token
        since = null

        setState {
            copy(
                    publicRooms = emptyList(),
                    asyncPublicRoomsRequest = Loading(),
                    hasMore = false,
                    roomDirectoryDisplayName = roomDirectoryData.displayName
            )
        }
    }

    fun loadMore() {
        if (currentTask == null) {
            setState {
                copy(
                        asyncPublicRoomsRequest = Loading()
                )
            }

            load()
        }
    }

    private fun load() {
        currentTask = session.getPublicRooms(roomDirectoryData.homeServer,
                PublicRoomsParams(
                        limit = PUBLIC_ROOMS_LIMIT,
                        filter = PublicRoomsFilter(searchTerm = currentFilter),
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
                    joiningRoomsIds = joiningRoomsIds.toMutableList().apply { add(publicRoom.roomId) }
            )
        }

        session.joinRoom(publicRoom.roomId, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            }

            override fun onFailure(failure: Throwable) {
                // Notify the user
                _joinRoomErrorLiveData.postValue(LiveEvent(failure))

                setState {
                    copy(
                            joiningRoomsIds = joiningRoomsIds.toMutableList().apply { remove(publicRoom.roomId) },
                            joiningErrorRoomsIds = joiningErrorRoomsIds.toMutableList().apply { add(publicRoom.roomId) }
                    )
                }
            }
        })
    }

}