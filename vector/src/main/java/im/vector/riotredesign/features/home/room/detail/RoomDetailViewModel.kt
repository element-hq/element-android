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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.command.CommandParser
import im.vector.riotredesign.features.command.ParsedCommand
import im.vector.riotredesign.features.home.room.VisibleRoomStore
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDisplayableEvents
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get
import java.util.concurrent.TimeUnit

class RoomDetailViewModel(initialState: RoomDetailViewState,
                          private val session: Session,
                          private val visibleRoomHolder: VisibleRoomStore
) : VectorViewModel<RoomDetailViewState>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val displayedEventsObservable = BehaviorRelay.create<RoomDetailActions.EventDisplayed>()
    private val timeline = room.createTimeline(eventId, TimelineDisplayableEvents.DISPLAYABLE_TYPES)

    companion object : MvRxViewModelFactory<RoomDetailViewModel, RoomDetailViewState> {

        const val PAGINATION_COUNT = 50

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDetailViewState): RoomDetailViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            val visibleRoomHolder = viewModelContext.activity.get<VisibleRoomStore>()
            return RoomDetailViewModel(state, currentSession, visibleRoomHolder)
        }
    }

    init {
        observeRoomSummary()
        observeEventDisplayedActions()
        room.loadRoomMembersIfNeeded()
        timeline.start()
        setState { copy(timeline = this@RoomDetailViewModel.timeline) }
    }

    fun process(action: RoomDetailActions) {
        when (action) {
            is RoomDetailActions.SendMessage -> handleSendMessage(action)
            is RoomDetailActions.IsDisplayed -> handleIsDisplayed()
            is RoomDetailActions.EventDisplayed -> handleEventDisplayed(action)
            is RoomDetailActions.LoadMore -> handleLoadMore(action)
        }
    }

    private val _sendMessageResultLiveData = MutableLiveData<LiveEvent<SendMessageResult>>()
    val sendMessageResultLiveData: LiveData<LiveEvent<SendMessageResult>>
        get() = _sendMessageResultLiveData

    // PRIVATE METHODS *****************************************************************************

    private fun handleSendMessage(action: RoomDetailActions.SendMessage) {
        // Handle slash command
        val slashCommandResult = CommandParser.parseSplashCommand(action.text)

        when (slashCommandResult) {
            is ParsedCommand.ErrorNotACommand -> {
                // Send the text message to the room
                room.sendTextMessage(action.text, callback = object : MatrixCallback<Event> {})
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.MessageSent))
            }
            is ParsedCommand.ErrorSyntax -> {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandError(slashCommandResult.command)))
            }
            is ParsedCommand.ErrorEmptySlashCommand -> {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandUnknown("/")))
            }
            is ParsedCommand.ErrorUnknownSlashCommand -> {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandUnknown(slashCommandResult.slashCommand)))
            }
            is ParsedCommand.Invite -> {
                handleInviteSlashCommand(slashCommandResult)
            }
            is ParsedCommand.SetUserPowerLevel -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.ClearScalarToken -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.SetMarkdown -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.UnbanUser -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.BanUser -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.KickUser -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.JoinRoom -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.PartRoom -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.SendEmote -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.ChangeTopic -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
            is ParsedCommand.ChangeDisplayName -> {
                // TODO
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
            }
        }
    }

    private fun handleInviteSlashCommand(invite: ParsedCommand.Invite) {
        _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandHandled))

        room.invite(invite.userId, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandResultOk))
            }

            override fun onFailure(failure: Throwable) {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandResultError(failure)))
            }
        })
    }

    private fun handleEventDisplayed(action: RoomDetailActions.EventDisplayed) {
        displayedEventsObservable.accept(action)
    }

    private fun handleIsDisplayed() {
        visibleRoomHolder.post(roomId)
    }

    private fun handleLoadMore(action: RoomDetailActions.LoadMore) {
        timeline.paginate(action.direction, PAGINATION_COUNT)
    }

    private fun observeEventDisplayedActions() {
        // We are buffering scroll events for one second
        // and keep the most recent one to set the read receipt on.
        displayedEventsObservable
                .buffer(1, TimeUnit.SECONDS)
                .filter { it.isNotEmpty() }
                .subscribeBy(onNext = { actions ->
                    val mostRecentEvent = actions.maxBy { it.event.displayIndex }
                    mostRecentEvent?.event?.root?.eventId?.let { eventId ->
                        room.setReadReceipt(eventId, callback = object : MatrixCallback<Unit> {})
                    }
                })
                .disposeOnClear()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .execute { async ->
                    copy(asyncRoomSummary = async)
                }
    }

    override fun onCleared() {
        timeline.dispose()
        super.onCleared()
    }
}