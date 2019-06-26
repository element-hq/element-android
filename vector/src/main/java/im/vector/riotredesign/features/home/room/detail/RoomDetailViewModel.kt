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

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.rx.rx
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.command.CommandParser
import im.vector.riotredesign.features.command.ParsedCommand
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDisplayableEvents
import io.reactivex.rxkotlin.subscribeBy
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.koin.android.ext.android.get
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class RoomDetailViewModel(initialState: RoomDetailViewState,
                          private val session: Session
) : VectorViewModel<RoomDetailViewState>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val displayedEventsObservable = BehaviorRelay.create<RoomDetailActions.EventDisplayed>()
    private val allowedTypes = if (TimelineDisplayableEvents.DEBUG_HIDDEN_EVENT) {
        TimelineDisplayableEvents.DEBUG_DISPLAYABLE_TYPES
    } else {
        TimelineDisplayableEvents.DISPLAYABLE_TYPES
    }
    private var timeline = room.createTimeline(eventId, allowedTypes)

    companion object : MvRxViewModelFactory<RoomDetailViewModel, RoomDetailViewState> {

        const val PAGINATION_COUNT = 50

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDetailViewState): RoomDetailViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            return RoomDetailViewModel(state, currentSession)
        }
    }

    init {
        observeRoomSummary()
        observeEventDisplayedActions()
        observeInvitationState()
        room.loadRoomMembersIfNeeded()
        timeline.start()
        setState { copy(timeline = this@RoomDetailViewModel.timeline) }
    }

    fun process(action: RoomDetailActions) {
        when (action) {
            is RoomDetailActions.SendMessage            -> handleSendMessage(action)
            is RoomDetailActions.SendMedia              -> handleSendMedia(action)
            is RoomDetailActions.EventDisplayed         -> handleEventDisplayed(action)
            is RoomDetailActions.LoadMore               -> handleLoadMore(action)
            is RoomDetailActions.SendReaction           -> handleSendReaction(action)
            is RoomDetailActions.AcceptInvite           -> handleAcceptInvite()
            is RoomDetailActions.RejectInvite           -> handleRejectInvite()
            is RoomDetailActions.RedactAction           -> handleRedactEvent(action)
            is RoomDetailActions.UndoReaction           -> handleUndoReact(action)
            is RoomDetailActions.UpdateQuickReactAction -> handleUpdateQuickReaction(action)
            is RoomDetailActions.ShowEditHistoryAction  -> handleShowEditHistoryReaction(action)
            is RoomDetailActions.EnterEditMode          -> handleEditAction(action)
            is RoomDetailActions.EnterQuoteMode         -> handleQuoteAction(action)
            is RoomDetailActions.EnterReplyMode         -> handleReplyAction(action)
            is RoomDetailActions.NavigateToEvent        -> handleNavigateToEvent(action)
            else                                        -> Timber.e("Unhandled Action: $action")
        }
    }

    fun enterEditMode(event: TimelineEvent) {
        setState {
            copy(
                    sendMode = SendMode.EDIT,
                    selectedEvent = event
            )
        }
    }

    fun resetSendMode() {
        setState {
            copy(
                    sendMode = SendMode.REGULAR,
                    selectedEvent = null
            )
        }
    }

    private val _nonBlockingPopAlert = MutableLiveData<LiveEvent<Pair<Int, List<Any>>>>()
    val nonBlockingPopAlert: LiveData<LiveEvent<Pair<Int, List<Any>>>>
        get() = _nonBlockingPopAlert


    private val _sendMessageResultLiveData = MutableLiveData<LiveEvent<SendMessageResult>>()
    val sendMessageResultLiveData: LiveData<LiveEvent<SendMessageResult>>
        get() = _sendMessageResultLiveData

    private val _navigateToEvent = MutableLiveData<LiveEvent<String>>()
    val navigateToEvent: LiveData<LiveEvent<String>>
        get() = _navigateToEvent


    // PRIVATE METHODS *****************************************************************************

    private fun handleSendMessage(action: RoomDetailActions.SendMessage) {
        withState { state ->
            when (state.sendMode) {
                SendMode.REGULAR -> {
                    val slashCommandResult = CommandParser.parseSplashCommand(action.text)

                    when (slashCommandResult) {
                        is ParsedCommand.ErrorNotACommand         -> {
                            // Send the text message to the room
                            room.sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.MessageSent))
                        }
                        is ParsedCommand.ErrorSyntax              -> {
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandError(slashCommandResult.command)))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand   -> {
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandUnknown("/")))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand -> {
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandUnknown(slashCommandResult.slashCommand)))
                        }
                        is ParsedCommand.Invite                   -> {
                            handleInviteSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.SetUserPowerLevel        -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.ClearScalarToken         -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.SetMarkdown              -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.UnbanUser                -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.BanUser                  -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.KickUser                 -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.JoinRoom                 -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.PartRoom                 -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                        is ParsedCommand.SendEmote                -> {
                            room.sendTextMessage(slashCommandResult.message, msgType = MessageType.MSGTYPE_EMOTE)
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandHandled))
                        }
                        is ParsedCommand.ChangeTopic              -> {
                            handleChangeTopicSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeDisplayName        -> {
                            // TODO
                            _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandNotImplemented))
                        }
                    }
                }
                SendMode.EDIT    -> {
                    room.editTextMessage(state.selectedEvent?.root?.eventId
                            ?: "", action.text, action.autoMarkdown)
                    setState {
                        copy(
                                sendMode = SendMode.REGULAR,
                                selectedEvent = null
                        )
                    }
                    _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.MessageSent))
                }
                SendMode.QUOTE   -> {
                    val messageContent: MessageContent? =
                            state.selectedEvent?.annotations?.editSummary?.aggregatedContent?.toModel()
                                    ?: state.selectedEvent?.root?.content.toModel()
                    val textMsg = messageContent?.body

                    val finalText = legacyRiotQuoteText(textMsg, action.text)

                    //TODO Refactor this, just temporary for quotes
                    val parser = Parser.builder().build()
                    val document = parser.parse(finalText)
                    val renderer = HtmlRenderer.builder().build()
                    val htmlText = renderer.render(document)
                    if (TextUtils.equals(finalText, htmlText)) {
                        room.sendTextMessage(finalText)
                    } else {
                        room.sendFormattedTextMessage(finalText, htmlText)
                    }
                    setState {
                        copy(
                                sendMode = SendMode.REGULAR,
                                selectedEvent = null
                        )
                    }
                    _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.MessageSent))
                }
                SendMode.REPLY   -> {
                    state.selectedEvent?.let {
                        room.replyToMessage(it.root, action.text)
                        setState {
                            copy(
                                    sendMode = SendMode.REGULAR,
                                    selectedEvent = null
                            )
                        }
                        _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.MessageSent))
                    }

                }
            }
        }
        // Handle slash command

    }

    private fun legacyRiotQuoteText(quotedText: String?, myText: String): String {
        val messageParagraphs = quotedText?.split("\n\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        var quotedTextMsg = StringBuilder()
        if (messageParagraphs != null) {
            for (i in messageParagraphs.indices) {
                if (messageParagraphs[i].trim({ it <= ' ' }) != "") {
                    quotedTextMsg.append("> ").append(messageParagraphs[i])
                }

                if (i + 1 != messageParagraphs.size) {
                    quotedTextMsg.append("\n\n")
                }
            }
        }
        val finalText = "$quotedTextMsg\n\n$myText"
        return finalText
    }

    private fun handleShowEditHistoryReaction(action: RoomDetailActions.ShowEditHistoryAction) {
        //TODO temporary implementation
        val lastReplace = action.editAggregatedSummary.sourceEvents.lastOrNull()?.let {
            room.getTimeLineEvent(it)
        } ?: return

        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
        _nonBlockingPopAlert.postValue(LiveEvent(
                Pair(R.string.last_edited_info_message, listOf(
                        lastReplace.getDisambiguatedDisplayName(),
                        dateFormat.format(Date(lastReplace.root.originServerTs ?: 0)))
                ))
        )
    }


    private fun handleChangeTopicSlashCommand(changeTopic: ParsedCommand.ChangeTopic) {
        _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandHandled))

        room.updateTopic(changeTopic.topic, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandResultOk))
            }

            override fun onFailure(failure: Throwable) {
                _sendMessageResultLiveData.postValue(LiveEvent(SendMessageResult.SlashCommandResultError(failure)))
            }
        })
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


    private fun handleSendReaction(action: RoomDetailActions.SendReaction) {
        room.sendReaction(action.reaction, action.targetEventId)
    }

    private fun handleRedactEvent(action: RoomDetailActions.RedactAction) {
        val event = room.getTimeLineEvent(action.targetEventId) ?: return
        room.redactEvent(event.root, action.reason)
    }

    private fun handleUndoReact(action: RoomDetailActions.UndoReaction) {
        room.undoReaction(action.key, action.targetEventId, session.sessionParams.credentials.userId)
    }


    private fun handleUpdateQuickReaction(action: RoomDetailActions.UpdateQuickReactAction) {
        if (action.add) {
            room.sendReaction(action.selectedReaction, action.targetEventId)
        } else {
            room.undoReaction(action.selectedReaction, action.targetEventId, session.sessionParams.credentials.userId)
        }
    }

    private fun handleSendMedia(action: RoomDetailActions.SendMedia) {
        val attachments = action.mediaFiles.map {
            ContentAttachmentData(
                    size = it.size,
                    duration = it.duration,
                    date = it.date,
                    height = it.height,
                    width = it.width,
                    name = it.name,
                    path = it.path,
                    mimeType = it.mimeType,
                    type = ContentAttachmentData.Type.values()[it.mediaType]
            )
        }
        room.sendMedias(attachments)
    }

    private fun handleEventDisplayed(action: RoomDetailActions.EventDisplayed) {
        displayedEventsObservable.accept(action)
        //We need to update this with the related m.replace also (to move read receipt)
        action.event.annotations?.editSummary?.sourceEvents?.forEach {
            room.getTimeLineEvent(it)?.let { event ->
                displayedEventsObservable.accept(RoomDetailActions.EventDisplayed(event))
            }
        }
    }

    private fun handleLoadMore(action: RoomDetailActions.LoadMore) {
        timeline.paginate(action.direction, PAGINATION_COUNT)
    }

    private fun handleRejectInvite() {
        room.leave(object : MatrixCallback<Unit> {})
    }

    private fun handleAcceptInvite() {
        room.join(object : MatrixCallback<Unit> {})
    }

    private fun handleEditAction(action: RoomDetailActions.EnterEditMode) {
        room.getTimeLineEvent(action.eventId)?.let {
            enterEditMode(it)
        }
    }

    private fun handleQuoteAction(action: RoomDetailActions.EnterQuoteMode) {
        room.getTimeLineEvent(action.eventId)?.let {
            setState {
                copy(
                        sendMode = SendMode.QUOTE,
                        selectedEvent = it
                )
            }
        }
    }

    private fun handleReplyAction(action: RoomDetailActions.EnterReplyMode) {
        room.getTimeLineEvent(action.eventId)?.let {
            setState {
                copy(
                        sendMode = SendMode.REPLY,
                        selectedEvent = it
                )
            }
        }
    }

    private fun handleNavigateToEvent(action: RoomDetailActions.NavigateToEvent) {
        val targetEventId = action.eventId

        if (action.position != null) {
            // Event is already in RAM
            withState {
                if (it.eventId == targetEventId) {
                    // ensure another click on the same permalink will also do a scroll
                    setState {
                        copy(
                                eventId = null
                        )
                    }
                }

                setState {
                    copy(
                            eventId = targetEventId
                    )
                }
            }

            _navigateToEvent.postValue(LiveEvent(targetEventId))
        } else {
            // change timeline
            timeline.dispose()
            timeline = room.createTimeline(targetEventId, allowedTypes)
            timeline.start()

            withState {
                if (it.eventId == targetEventId) {
                    // ensure another click on the same permalink will also do a scroll
                    setState {
                        copy(
                                eventId = null
                        )
                    }
                }

                setState {
                    copy(
                            eventId = targetEventId,
                            timeline = this@RoomDetailViewModel.timeline
                    )
                }
            }

            _navigateToEvent.postValue(LiveEvent(targetEventId))
        }
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
                    copy(
                            asyncRoomSummary = async,
                            isEncrypted = room.isEncrypted()
                    )
                }
    }

    private fun observeInvitationState() {
        asyncSubscribe(RoomDetailViewState::asyncRoomSummary) { summary ->
            if (summary.membership == Membership.INVITE) {
                summary.lastMessage?.senderId?.let { senderId ->
                    session.getUser(senderId)
                }?.also {
                    setState { copy(asyncInviter = Success(it)) }
                }
            }
        }
    }

    override fun onCleared() {
        timeline.dispose()
        super.onCleared()
    }

}