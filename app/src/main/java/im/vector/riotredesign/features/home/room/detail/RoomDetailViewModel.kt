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

package im.vector.riotredesign.features.home.room.detail

import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.features.home.room.VisibleRoomStore
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get
import timber.log.Timber
import java.util.concurrent.TimeUnit

class RoomDetailViewModel(initialState: RoomDetailViewState,
                          private val session: Session,
                          private val visibleRoomHolder: VisibleRoomStore
) : RiotViewModel<RoomDetailViewState>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val displayedEventsObservable = BehaviorRelay.create<RoomDetailActions.EventDisplayed>()
    private val timeline = room.createTimeline(eventId)

    companion object : MvRxViewModelFactory<RoomDetailViewModel, RoomDetailViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDetailViewState): RoomDetailViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            val visibleRoomHolder = viewModelContext.activity.get<VisibleRoomStore>()
            return RoomDetailViewModel(state, currentSession, visibleRoomHolder)
        }
    }

    init {
        observeRoomSummary()
        observeDisplayedEvents()
        room.loadRoomMembersIfNeeded()
        timeline.start()
        setState { copy(timeline = this@RoomDetailViewModel.timeline) }
    }

    fun process(action: RoomDetailActions) {
        when (action) {
            is RoomDetailActions.SendMessage    -> handleSendMessage(action)
            is RoomDetailActions.IsDisplayed    -> handleIsDisplayed()
            is RoomDetailActions.EventDisplayed -> handleEventDisplayed(action)
            is RoomDetailActions.LoadMore       -> handleLoadMore(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSendMessage(action: RoomDetailActions.SendMessage) {
        room.sendTextMessage(action.text, callback = object : MatrixCallback<Event> {})
    }

    private fun handleEventDisplayed(action: RoomDetailActions.EventDisplayed) {
        displayedEventsObservable.accept(action)
    }

    private fun handleIsDisplayed() {
        visibleRoomHolder.post(roomId)
    }

    private fun handleLoadMore(action: RoomDetailActions.LoadMore) {
        timeline.paginate(action.direction, 50)
    }

    private fun observeDisplayedEvents() {
        // We are buffering scroll events for one second
        // and keep the most recent one to set the read receipt on.
        displayedEventsObservable
                .buffer(1, TimeUnit.SECONDS)
                .filter { it.isNotEmpty() }
                .subscribeBy(onNext = { actions ->
                    val mostRecentEvent = actions.maxBy { it.event.displayIndex }
                    mostRecentEvent?.event?.root?.eventId?.let { eventId ->
                        room.setReadReceipt(eventId, callback = object : MatrixCallback<Void> {})
                    }
                })
                .disposeOnClear()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .execute { async ->
                    Timber.v("Room summary updated: $async")
                    copy(asyncRoomSummary = async)
                }
    }

    override fun onCleared() {
        timeline.dispose()
        super.onCleared()
    }

}