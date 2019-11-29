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

package im.vector.riotx.features.home.room.detail

import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.*
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.isImageMessage
import im.vector.matrix.android.api.session.events.model.isTextMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilities
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.message.getFileUrl
import im.vector.matrix.android.api.session.room.model.tombstone.RoomTombstoneContent
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.api.session.room.timeline.getTextEditableContent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.resources.UserPreferencesProvider
import im.vector.riotx.core.utils.LiveEvent
import im.vector.riotx.core.utils.subscribeLogError
import im.vector.riotx.features.command.CommandParser
import im.vector.riotx.features.command.ParsedCommand
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineDisplayableEvents
import im.vector.riotx.features.settings.VectorPreferences
import io.reactivex.rxkotlin.subscribeBy
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class RoomDetailViewModel @AssistedInject constructor(@Assisted initialState: RoomDetailViewState,
                                                      userPreferencesProvider: UserPreferencesProvider,
                                                      private val vectorPreferences: VectorPreferences,
                                                      private val stringProvider: StringProvider,
                                                      private val session: Session
) : VectorViewModel<RoomDetailViewState, RoomDetailAction>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val eventId = initialState.eventId
    private val invisibleEventsObservable = BehaviorRelay.create<RoomDetailAction.TimelineEventTurnsInvisible>()
    private val visibleEventsObservable = BehaviorRelay.create<RoomDetailAction.TimelineEventTurnsVisible>()
    private val timelineSettings = if (userPreferencesProvider.shouldShowHiddenEvents()) {
        TimelineSettings(30,
                filterEdits = false,
                filterTypes = true,
                allowedTypes = TimelineDisplayableEvents.DEBUG_DISPLAYABLE_TYPES,
                buildReadReceipts = userPreferencesProvider.shouldShowReadReceipts())
    } else {
        TimelineSettings(30,
                filterEdits = true,
                filterTypes = true,
                allowedTypes = TimelineDisplayableEvents.DISPLAYABLE_TYPES,
                buildReadReceipts = userPreferencesProvider.shouldShowReadReceipts())
    }

    private var timeline = room.createTimeline(eventId, timelineSettings)

    // Can be used for several actions, for a one shot result
    private val _requestLiveData = MutableLiveData<LiveEvent<Async<RoomDetailAction>>>()
    val requestLiveData: LiveData<LiveEvent<Async<RoomDetailAction>>>
        get() = _requestLiveData

    // Slot to keep a pending action during permission request
    var pendingAction: RoomDetailAction? = null
    // Slot to keep a pending uri during permission request
    var pendingUri: Uri? = null

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomDetailViewState): RoomDetailViewModel
    }

    companion object : MvRxViewModelFactory<RoomDetailViewModel, RoomDetailViewState> {

        const val PAGINATION_COUNT = 50

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDetailViewState): RoomDetailViewModel? {
            val fragment: RoomDetailFragment = (viewModelContext as FragmentViewModelContext).fragment()

            return fragment.roomDetailViewModelFactory.create(state)
        }
    }

    init {
        observeSyncState()
        observeRoomSummary()
        observeEventDisplayedActions()
        observeSummaryState()
        observeDrafts()
        room.rx().loadRoomMembersIfNeeded().subscribeLogError().disposeOnClear()
        timeline.start()
        setState { copy(timeline = this@RoomDetailViewModel.timeline) }
    }

    override fun handle(action: RoomDetailAction) {
        when (action) {
            is RoomDetailAction.SaveDraft                   -> handleSaveDraft(action)
            is RoomDetailAction.SendMessage                 -> handleSendMessage(action)
            is RoomDetailAction.SendMedia                   -> handleSendMedia(action)
            is RoomDetailAction.TimelineEventTurnsVisible   -> handleEventVisible(action)
            is RoomDetailAction.TimelineEventTurnsInvisible -> handleEventInvisible(action)
            is RoomDetailAction.LoadMoreTimelineEvents      -> handleLoadMore(action)
            is RoomDetailAction.SendReaction                -> handleSendReaction(action)
            is RoomDetailAction.AcceptInvite                -> handleAcceptInvite()
            is RoomDetailAction.RejectInvite                -> handleRejectInvite()
            is RoomDetailAction.RedactAction                -> handleRedactEvent(action)
            is RoomDetailAction.UndoReaction                -> handleUndoReact(action)
            is RoomDetailAction.UpdateQuickReactAction      -> handleUpdateQuickReaction(action)
            is RoomDetailAction.ExitSpecialMode             -> handleExitSpecialMode(action)
            is RoomDetailAction.EnterEditMode               -> handleEditAction(action)
            is RoomDetailAction.EnterQuoteMode              -> handleQuoteAction(action)
            is RoomDetailAction.EnterReplyMode              -> handleReplyAction(action)
            is RoomDetailAction.DownloadFile                -> handleDownloadFile(action)
            is RoomDetailAction.NavigateToEvent             -> handleNavigateToEvent(action)
            is RoomDetailAction.HandleTombstoneEvent        -> handleTombstoneEvent(action)
            is RoomDetailAction.ResendMessage               -> handleResendEvent(action)
            is RoomDetailAction.RemoveFailedEcho            -> handleRemove(action)
            is RoomDetailAction.ClearSendQueue              -> handleClearSendQueue()
            is RoomDetailAction.ResendAll                   -> handleResendAll()
            is RoomDetailAction.SetReadMarkerAction         -> handleSetReadMarkerAction(action)
            is RoomDetailAction.MarkAllAsRead               -> handleMarkAllAsRead()
            is RoomDetailAction.ReportContent               -> handleReportContent(action)
            is RoomDetailAction.IgnoreUser                  -> handleIgnoreUser(action)
        }
    }

    private fun handleEventInvisible(action: RoomDetailAction.TimelineEventTurnsInvisible) {
        invisibleEventsObservable.accept(action)
    }

    fun getMember(userId: String) : RoomMember? {
       return room.getRoomMember(userId)
    }
    /**
     * Convert a send mode to a draft and save the draft
     */
    private fun handleSaveDraft(action: RoomDetailAction.SaveDraft) {
        withState {
            when (it.sendMode) {
                is SendMode.REGULAR -> room.saveDraft(UserDraft.REGULAR(action.draft))
                is SendMode.REPLY   -> room.saveDraft(UserDraft.REPLY(it.sendMode.timelineEvent.root.eventId!!, action.draft))
                is SendMode.QUOTE   -> room.saveDraft(UserDraft.QUOTE(it.sendMode.timelineEvent.root.eventId!!, action.draft))
                is SendMode.EDIT    -> room.saveDraft(UserDraft.EDIT(it.sendMode.timelineEvent.root.eventId!!, action.draft))
            }
        }
    }

    private fun observeDrafts() {
        room.rx().liveDrafts()
                .subscribe {
                    Timber.d("Draft update --> SetState")
                    setState {
                        val draft = it.lastOrNull() ?: UserDraft.REGULAR("")
                        copy(
                                // Create a sendMode from a draft and retrieve the TimelineEvent
                                sendMode = when (draft) {
                                    is UserDraft.REGULAR -> SendMode.REGULAR(draft.text)
                                    is UserDraft.QUOTE   -> {
                                        room.getTimeLineEvent(draft.linkedEventId)?.let { timelineEvent ->
                                            SendMode.QUOTE(timelineEvent, draft.text)
                                        }
                                    }
                                    is UserDraft.REPLY   -> {
                                        room.getTimeLineEvent(draft.linkedEventId)?.let { timelineEvent ->
                                            SendMode.REPLY(timelineEvent, draft.text)
                                        }
                                    }
                                    is UserDraft.EDIT    -> {
                                        room.getTimeLineEvent(draft.linkedEventId)?.let { timelineEvent ->
                                            SendMode.EDIT(timelineEvent, draft.text)
                                        }
                                    }
                                } ?: SendMode.REGULAR("")
                        )
                    }
                }
                .disposeOnClear()
    }

    private fun handleTombstoneEvent(action: RoomDetailAction.HandleTombstoneEvent) {
        val tombstoneContent = action.event.getClearContent().toModel<RoomTombstoneContent>()
                ?: return

        val roomId = tombstoneContent.replacementRoom ?: ""
        val isRoomJoined = session.getRoom(roomId)?.roomSummary()?.membership == Membership.JOIN
        if (isRoomJoined) {
            setState { copy(tombstoneEventHandling = Success(roomId)) }
        } else {
            val viaServer = MatrixPatterns.extractServerNameFromId(action.event.senderId).let {
                if (it.isNullOrBlank()) {
                    emptyList()
                } else {
                    listOf(it)
                }
            }
            session.rx()
                    .joinRoom(roomId, viaServer)
                    .map { roomId }
                    .execute {
                        copy(tombstoneEventHandling = it)
                    }
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

    private val _fileTooBigEvent = MutableLiveData<LiveEvent<FileTooBigError>>()
    val fileTooBigEvent: LiveData<LiveEvent<FileTooBigError>>
        get() = _fileTooBigEvent

    private val _downloadedFileEvent = MutableLiveData<LiveEvent<DownloadFileState>>()
    val downloadedFileEvent: LiveData<LiveEvent<DownloadFileState>>
        get() = _downloadedFileEvent

    fun isMenuItemVisible(@IdRes itemId: Int) = when (itemId) {
        R.id.clear_message_queue ->
            /* For now always disable on production, worker cancellation is not working properly */
            timeline.pendingEventCount() > 0 && BuildConfig.DEBUG
        R.id.resend_all          -> timeline.failedToDeliverEventCount() > 0
        R.id.clear_all           -> timeline.failedToDeliverEventCount() > 0
        else                     -> false
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSendMessage(action: RoomDetailAction.SendMessage) {
        withState { state ->
            when (state.sendMode) {
                is SendMode.REGULAR -> {
                    when (val slashCommandResult = CommandParser.parseSplashCommand(action.text)) {
                        is ParsedCommand.ErrorNotACommand         -> {
                            // Send the text message to the room
                            room.sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ErrorSyntax              -> {
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandError(slashCommandResult.command))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand   -> {
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandUnknown("/"))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand -> {
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandUnknown(slashCommandResult.slashCommand))
                        }
                        is ParsedCommand.Invite                   -> {
                            handleInviteSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.SetUserPowerLevel        -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.ClearScalarToken         -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown              -> {
                            vectorPreferences.setMarkdownEnabled(slashCommandResult.enable)
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandHandled(
                                    if (slashCommandResult.enable) R.string.markdown_has_been_enabled else R.string.markdown_has_been_disabled))
                            popDraft()
                        }
                        is ParsedCommand.UnbanUser                -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.BanUser                  -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.KickUser                 -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.JoinRoom                 -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.PartRoom                 -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SendEmote                -> {
                            room.sendTextMessage(slashCommandResult.message, msgType = MessageType.MSGTYPE_EMOTE)
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandHandled())
                            popDraft()
                        }
                        is ParsedCommand.SendSpoiler              -> {
                            room.sendFormattedTextMessage(
                                    "[${stringProvider.getString(R.string.spoiler)}](${slashCommandResult.message})",
                                    "<span data-mx-spoiler>${slashCommandResult.message}</span>"
                            )
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandHandled())
                            popDraft()
                        }
                        is ParsedCommand.ChangeTopic              -> {
                            handleChangeTopicSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.ChangeDisplayName        -> {
                            // TODO
                            _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandNotImplemented)
                        }
                    }
                }
                is SendMode.EDIT    -> {
                    // is original event a reply?
                    val inReplyTo = state.sendMode.timelineEvent.root.getClearContent().toModel<MessageContent>()?.relatesTo?.inReplyTo?.eventId
                            ?: state.sendMode.timelineEvent.root.content.toModel<EncryptedEventContent>()?.relatesTo?.inReplyTo?.eventId
                    if (inReplyTo != null) {
                        // TODO check if same content?
                        room.getTimeLineEvent(inReplyTo)?.let {
                            room.editReply(state.sendMode.timelineEvent, it, action.text.toString())
                        }
                    } else {
                        val messageContent: MessageContent? =
                                state.sendMode.timelineEvent.annotations?.editSummary?.aggregatedContent.toModel()
                                        ?: state.sendMode.timelineEvent.root.getClearContent().toModel()
                        val existingBody = messageContent?.body ?: ""
                        if (existingBody != action.text) {
                            room.editTextMessage(state.sendMode.timelineEvent.root.eventId ?: "",
                                    messageContent?.type ?: MessageType.MSGTYPE_TEXT,
                                    action.text,
                                    action.autoMarkdown)
                        } else {
                            Timber.w("Same message content, do not send edition")
                        }
                    }
                    _sendMessageResultLiveData.postLiveEvent(SendMessageResult.MessageSent)
                    popDraft()
                }
                is SendMode.QUOTE   -> {
                    val messageContent: MessageContent? =
                            state.sendMode.timelineEvent.annotations?.editSummary?.aggregatedContent.toModel()
                                    ?: state.sendMode.timelineEvent.root.getClearContent().toModel()
                    val textMsg = messageContent?.body

                    val finalText = legacyRiotQuoteText(textMsg, action.text.toString())

                    // TODO check for pills?

                    // TODO Refactor this, just temporary for quotes
                    val parser = Parser.builder().build()
                    val document = parser.parse(finalText)
                    val renderer = HtmlRenderer.builder().build()
                    val htmlText = renderer.render(document)
                    if (finalText == htmlText) {
                        room.sendTextMessage(finalText)
                    } else {
                        room.sendFormattedTextMessage(finalText, htmlText)
                    }
                    _sendMessageResultLiveData.postLiveEvent(SendMessageResult.MessageSent)
                    popDraft()
                }
                is SendMode.REPLY   -> {
                    state.sendMode.timelineEvent.let {
                        room.replyToMessage(it, action.text.toString(), action.autoMarkdown)
                        _sendMessageResultLiveData.postLiveEvent(SendMessageResult.MessageSent)
                        popDraft()
                    }
                }
            }
        }
    }

    private fun popDraft() {
        room.deleteDraft()
    }

    private fun legacyRiotQuoteText(quotedText: String?, myText: String): String {
        val messageParagraphs = quotedText?.split("\n\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        return buildString {
            if (messageParagraphs != null) {
                for (i in messageParagraphs.indices) {
                    if (messageParagraphs[i].isNotBlank()) {
                        append("> ")
                        append(messageParagraphs[i])
                    }

                    if (i != messageParagraphs.lastIndex) {
                        append("\n\n")
                    }
                }
            }
            append("\n\n")
            append(myText)
        }
    }

    private fun handleChangeTopicSlashCommand(changeTopic: ParsedCommand.ChangeTopic) {
        _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandHandled())

        room.updateTopic(changeTopic.topic, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandResultOk)
            }

            override fun onFailure(failure: Throwable) {
                _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandResultError(failure))
            }
        })
    }

    private fun handleInviteSlashCommand(invite: ParsedCommand.Invite) {
        _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandHandled())

        room.invite(invite.userId, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandResultOk)
            }

            override fun onFailure(failure: Throwable) {
                _sendMessageResultLiveData.postLiveEvent(SendMessageResult.SlashCommandResultError(failure))
            }
        })
    }

    private fun handleSendReaction(action: RoomDetailAction.SendReaction) {
        room.sendReaction(action.targetEventId, action.reaction)
    }

    private fun handleRedactEvent(action: RoomDetailAction.RedactAction) {
        val event = room.getTimeLineEvent(action.targetEventId) ?: return
        room.redactEvent(event.root, action.reason)
    }

    private fun handleUndoReact(action: RoomDetailAction.UndoReaction) {
        room.undoReaction(action.targetEventId, action.reaction)
    }

    private fun handleUpdateQuickReaction(action: RoomDetailAction.UpdateQuickReactAction) {
        if (action.add) {
            room.sendReaction(action.targetEventId, action.selectedReaction)
        } else {
            room.undoReaction(action.targetEventId, action.selectedReaction)
        }
    }

    private fun handleSendMedia(action: RoomDetailAction.SendMedia) {
        val attachments = action.attachments
        val homeServerCapabilities = session.getHomeServerCapabilities()
        val maxUploadFileSize = homeServerCapabilities.maxUploadFileSize

        if (maxUploadFileSize == HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN) {
            // Unknown limitation
            room.sendMedias(attachments)
        } else {
            when (val tooBigFile = attachments.find { it.size > maxUploadFileSize }) {
                null -> room.sendMedias(attachments)
                else -> _fileTooBigEvent.postValue(LiveEvent(FileTooBigError(tooBigFile.name
                        ?: tooBigFile.path, tooBigFile.size, maxUploadFileSize)))
            }
        }
    }

    private fun handleEventVisible(action: RoomDetailAction.TimelineEventTurnsVisible) {
        if (action.event.root.sendState.isSent()) { // ignore pending/local events
            visibleEventsObservable.accept(action)
        }
        // We need to update this with the related m.replace also (to move read receipt)
        action.event.annotations?.editSummary?.sourceEvents?.forEach {
            room.getTimeLineEvent(it)?.let { event ->
                visibleEventsObservable.accept(RoomDetailAction.TimelineEventTurnsVisible(event))
            }
        }
    }

    private fun handleLoadMore(action: RoomDetailAction.LoadMoreTimelineEvents) {
        timeline.paginate(action.direction, PAGINATION_COUNT)
    }

    private fun handleRejectInvite() {
        room.leave(object : MatrixCallback<Unit> {})
    }

    private fun handleAcceptInvite() {
        room.join(callback = object : MatrixCallback<Unit> {})
    }

    private fun handleEditAction(action: RoomDetailAction.EnterEditMode) {
        saveCurrentDraft(action.text)

        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.EDIT(timelineEvent, action.text)) }
            timelineEvent.root.eventId?.let {
                room.saveDraft(UserDraft.EDIT(it, timelineEvent.getTextEditableContent() ?: ""))
            }
        }
    }

    private fun handleQuoteAction(action: RoomDetailAction.EnterQuoteMode) {
        saveCurrentDraft(action.text)

        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.QUOTE(timelineEvent, action.text)) }
            withState { state ->
                // Save a new draft and keep the previously entered text, if it was not an edit
                timelineEvent.root.eventId?.let {
                    if (state.sendMode is SendMode.EDIT) {
                        room.saveDraft(UserDraft.QUOTE(it, ""))
                    } else {
                        room.saveDraft(UserDraft.QUOTE(it, action.text))
                    }
                }
            }
        }
    }

    private fun handleReplyAction(action: RoomDetailAction.EnterReplyMode) {
        saveCurrentDraft(action.text)

        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.REPLY(timelineEvent, action.text)) }
            withState { state ->
                // Save a new draft and keep the previously entered text, if it was not an edit
                timelineEvent.root.eventId?.let {
                    if (state.sendMode is SendMode.EDIT) {
                        room.saveDraft(UserDraft.REPLY(it, ""))
                    } else {
                        room.saveDraft(UserDraft.REPLY(it, action.text))
                    }
                }
            }
        }
    }

    private fun saveCurrentDraft(draft: String) {
        // Save the draft with the current text if any
        withState {
            if (draft.isNotBlank()) {
                when (it.sendMode) {
                    is SendMode.REGULAR -> room.saveDraft(UserDraft.REGULAR(draft))
                    is SendMode.REPLY   -> room.saveDraft(UserDraft.REPLY(it.sendMode.timelineEvent.root.eventId!!, draft))
                    is SendMode.QUOTE   -> room.saveDraft(UserDraft.QUOTE(it.sendMode.timelineEvent.root.eventId!!, draft))
                    is SendMode.EDIT    -> room.saveDraft(UserDraft.EDIT(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
            }
        }
    }

    private fun handleExitSpecialMode(action: RoomDetailAction.ExitSpecialMode) {
        setState { copy(sendMode = SendMode.REGULAR(action.text)) }
        withState { state ->
            // For edit, just delete the current draft
            if (state.sendMode is SendMode.EDIT) {
                room.deleteDraft()
            } else {
                // Save a new draft and keep the previously entered text
                room.saveDraft(UserDraft.REGULAR(action.text))
            }
        }
    }

    private fun handleDownloadFile(action: RoomDetailAction.DownloadFile) {
        session.downloadFile(
                FileService.DownloadMode.TO_EXPORT,
                action.eventId,
                action.messageFileContent.getFileName(),
                action.messageFileContent.getFileUrl(),
                action.messageFileContent.encryptedFileInfo?.toElementToDecrypt(),
                object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        _downloadedFileEvent.postLiveEvent(DownloadFileState(
                                action.messageFileContent.getMimeType(),
                                data,
                                null
                        ))
                    }

                    override fun onFailure(failure: Throwable) {
                        _downloadedFileEvent.postLiveEvent(DownloadFileState(
                                action.messageFileContent.getMimeType(),
                                null,
                                failure
                        ))
                    }
                })
    }

    private fun handleNavigateToEvent(action: RoomDetailAction.NavigateToEvent) {
        val targetEventId: String = action.eventId
        val correctedEventId = timeline.getFirstDisplayableEventId(targetEventId) ?: targetEventId
        val indexOfEvent = timeline.getIndexOfEvent(correctedEventId)
        if (indexOfEvent == null) {
            // Event is not already in RAM
            timeline.restartWithEventId(targetEventId)
        }
        if (action.highlight) {
            setState { copy(highlightedEventId = correctedEventId) }
        }
        _navigateToEvent.postLiveEvent(correctedEventId)
    }

    private fun handleResendEvent(action: RoomDetailAction.ResendMessage) {
        val targetEventId = action.eventId
        room.getTimeLineEvent(targetEventId)?.let {
            // State must be UNDELIVERED or Failed
            if (!it.root.sendState.hasFailed()) {
                Timber.e("Cannot resend message, it is not failed, Cancel first")
                return
            }
            when {
                it.root.isTextMessage()  -> room.resendTextMessage(it)
                it.root.isImageMessage() -> room.resendMediaMessage(it)
                else                     -> {
                    // TODO
                }
            }
        }
    }

    private fun handleRemove(action: RoomDetailAction.RemoveFailedEcho) {
        val targetEventId = action.eventId
        room.getTimeLineEvent(targetEventId)?.let {
            // State must be UNDELIVERED or Failed
            if (!it.root.sendState.hasFailed()) {
                Timber.e("Cannot resend message, it is not failed, Cancel first")
                return
            }
            room.deleteFailedEcho(it)
        }
    }

    private fun handleClearSendQueue() {
        room.clearSendingQueue()
    }

    private fun handleResendAll() {
        room.resendAllFailedMessages()
    }

    private fun observeEventDisplayedActions() {
        // We are buffering scroll events for one second
        // and keep the most recent one to set the read receipt on.
        visibleEventsObservable
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

    private fun handleSetReadMarkerAction(action: RoomDetailAction.SetReadMarkerAction) = withState {
        var readMarkerId = action.eventId
        val indexOfEvent = timeline.getIndexOfEvent(readMarkerId)
        // force to set the read marker on the next event
        if (indexOfEvent != null) {
            timeline.getTimelineEventAtIndex(indexOfEvent - 1)?.root?.eventId?.also { eventIdOfNext ->
                readMarkerId = eventIdOfNext
            }
        }
        room.setReadMarker(readMarkerId, callback = object : MatrixCallback<Unit> {})
    }

    private fun handleMarkAllAsRead() {
        room.markAllAsRead(object : MatrixCallback<Any> {})
    }

    private fun handleReportContent(action: RoomDetailAction.ReportContent) {
        room.reportContent(action.eventId, -100, action.reason, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _requestLiveData.postValue(LiveEvent(Success(action)))
            }

            override fun onFailure(failure: Throwable) {
                _requestLiveData.postValue(LiveEvent(Fail(failure)))
            }
        })
    }

    private fun handleIgnoreUser(action: RoomDetailAction.IgnoreUser) {
        if (action.userId.isNullOrEmpty()) {
            return
        }

        session.ignoreUserIds(listOf(action.userId), object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _requestLiveData.postValue(LiveEvent(Success(action)))
            }

            override fun onFailure(failure: Throwable) {
                _requestLiveData.postValue(LiveEvent(Fail(failure)))
            }
        })
    }

    private fun observeSyncState() {
        session.rx()
                .liveSyncState()
                .subscribe { syncState ->
                    setState {
                        copy(syncState = syncState)
                    }
                }
                .disposeOnClear()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(
                            asyncRoomSummary = async,
                            isEncrypted = room.isEncrypted()
                    )
                }
    }

    private fun observeSummaryState() {
        asyncSubscribe(RoomDetailViewState::asyncRoomSummary) { summary ->
            if (summary.membership == Membership.INVITE) {
                summary.latestPreviewableEvent?.root?.senderId?.let { senderId ->
                    session.getUser(senderId)
                }?.also {
                    setState { copy(asyncInviter = Success(it)) }
                }
            }
            room.getStateEvent(EventType.STATE_ROOM_TOMBSTONE)?.also {
                setState { copy(tombstoneEvent = it) }
            }
        }
    }

    override fun onCleared() {
        timeline.dispose()
        super.onCleared()
    }
}
