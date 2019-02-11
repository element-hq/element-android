/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.home.room.list.RoomSelectionRepository
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get

class EmptyState : MvRxState

class HomeActivityViewModel(state: EmptyState,
                            private val session: Session,
                            roomSelectionRepository: RoomSelectionRepository
) : RiotViewModel<EmptyState>(state) {

    companion object : MvRxViewModelFactory<HomeActivityViewModel, EmptyState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: EmptyState): HomeActivityViewModel? {
            val session = Matrix.getInstance().currentSession
            val roomSelectionRepository = viewModelContext.activity.get<RoomSelectionRepository>()
            return HomeActivityViewModel(state, session, roomSelectionRepository)
        }
    }

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    init {
        val lastSelectedRoomId = roomSelectionRepository.lastSelectedRoom()
        if (lastSelectedRoomId == null || session.getRoom(lastSelectedRoomId) == null) {
            getTheFirstRoomWhenAvailable()
        } else {
            _openRoomLiveData.postValue(LiveEvent(lastSelectedRoomId))
        }
    }

    private fun getTheFirstRoomWhenAvailable() {
        session.rx().liveRoomSummaries()
                .filter { it.isNotEmpty() }
                .first(emptyList())
                .subscribeBy {
                    val firstRoom = it.firstOrNull()
                    if (firstRoom != null) {
                        _openRoomLiveData.postValue(LiveEvent(firstRoom.roomId))
                    }
                }
                .disposeOnClear()
    }


}