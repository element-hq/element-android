/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsComposer
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.attachments.toContentAttachmentData
import im.vector.app.features.command.CommandParser
import im.vector.app.features.command.ParsedCommand
import im.vector.app.features.home.room.detail.ChatEffect
import im.vector.app.features.home.room.detail.composer.rainbow.RainbowGenerator
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import im.vector.app.features.home.room.detail.toMessageType
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.voice.VoicePlayerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomEncryptionAlgorithm
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.shouldRenderInThread
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getRelationContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber

class MessageComposerViewModel @AssistedInject constructor(
        @Assisted initialState: MessageComposerViewState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val commandParser: CommandParser,
        private val rainbowGenerator: RainbowGenerator,
        private val audioMessageHelper: AudioMessageHelper,
        private val analyticsTracker: AnalyticsTracker,
        private val voicePlayerHelper: VoicePlayerHelper
) : VectorViewModel<MessageComposerViewState, MessageComposerAction, MessageComposerViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    // Keep it out of state to avoid invalidate being called
    private var currentComposerText: CharSequence = ""

    init {
        loadDraftIfAny()
        observePowerLevelAndEncryption()
        subscribeToStateInternal()
    }

    override fun handle(action: MessageComposerAction) {
        when (action) {
            is MessageComposerAction.EnterEditMode                  -> handleEnterEditMode(action)
            is MessageComposerAction.EnterQuoteMode                 -> handleEnterQuoteMode(action)
            is MessageComposerAction.EnterRegularMode               -> handleEnterRegularMode(action)
            is MessageComposerAction.EnterReplyMode                 -> handleEnterReplyMode(action)
            is MessageComposerAction.SendMessage                    -> handleSendMessage(action)
            is MessageComposerAction.UserIsTyping                   -> handleUserIsTyping(action)
            is MessageComposerAction.OnTextChanged                  -> handleOnTextChanged(action)
            is MessageComposerAction.OnVoiceRecordingUiStateChanged -> handleOnVoiceRecordingUiStateChanged(action)
            is MessageComposerAction.StartRecordingVoiceMessage     -> handleStartRecordingVoiceMessage()
            is MessageComposerAction.EndRecordingVoiceMessage       -> handleEndRecordingVoiceMessage(action.isCancelled, action.rootThreadEventId)
            is MessageComposerAction.PlayOrPauseVoicePlayback       -> handlePlayOrPauseVoicePlayback(action)
            MessageComposerAction.PauseRecordingVoiceMessage        -> handlePauseRecordingVoiceMessage()
            MessageComposerAction.PlayOrPauseRecordingPlayback      -> handlePlayOrPauseRecordingPlayback()
            is MessageComposerAction.EndAllVoiceActions             -> handleEndAllVoiceActions(action.deleteRecord)
            is MessageComposerAction.InitializeVoiceRecorder        -> handleInitializeVoiceRecorder(action.attachmentData)
            is MessageComposerAction.OnEntersBackground             -> handleEntersBackground(action.composerText)
            is MessageComposerAction.VoiceWaveformTouchedUp         -> handleVoiceWaveformTouchedUp(action)
            is MessageComposerAction.VoiceWaveformMovedTo           -> handleVoiceWaveformMovedTo(action)
            is MessageComposerAction.AudioSeekBarMovedTo            -> handleAudioSeekBarMovedTo(action)
        }
    }

    private fun handleOnVoiceRecordingUiStateChanged(action: MessageComposerAction.OnVoiceRecordingUiStateChanged) = setState {
        copy(voiceRecordingUiState = action.uiState)
    }

    private fun handleOnTextChanged(action: MessageComposerAction.OnTextChanged) {
        setState {
            // Makes sure currentComposerText is upToDate when accessing further setState
            currentComposerText = action.text
            this
        }
        updateIsSendButtonVisibility(true)
    }

    private fun subscribeToStateInternal() {
        onEach(MessageComposerViewState::sendMode, MessageComposerViewState::canSendMessage, MessageComposerViewState::isVoiceRecording) { _, _, _ ->
            updateIsSendButtonVisibility(false)
        }
    }

    private fun updateIsSendButtonVisibility(triggerAnimation: Boolean) = setState {
        val isSendButtonVisible = isComposerVisible && (sendMode !is SendMode.Regular || currentComposerText.isNotBlank())
        if (this.isSendButtonVisible != isSendButtonVisible && triggerAnimation) {
            _viewEvents.post(MessageComposerViewEvents.AnimateSendButtonVisibility(isSendButtonVisible))
        }
        copy(isSendButtonVisible = isSendButtonVisible)
    }

    private fun handleEnterRegularMode(action: MessageComposerAction.EnterRegularMode) = setState {
        copy(sendMode = SendMode.Regular(action.text, action.fromSharing))
    }

    private fun handleEnterEditMode(action: MessageComposerAction.EnterEditMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Edit(timelineEvent, timelineEvent.getTextEditableContent())) }
        }
    }

    private fun observePowerLevelAndEncryption() {
        combine(
                PowerLevelsFlowFactory(room).createFlow(),
                room.flow().liveRoomSummary().unwrap()
        ) { pl, sum ->
            val canSendMessage = PowerLevelsHelper(pl).isUserAllowedToSend(session.myUserId, false, EventType.MESSAGE)
            if (canSendMessage) {
                val isE2E = sum.isEncrypted
                if (isE2E) {
                    val roomEncryptionAlgorithm = sum.roomEncryptionAlgorithm
                    if (roomEncryptionAlgorithm is RoomEncryptionAlgorithm.UnsupportedAlgorithm) {
                        CanSendStatus.UnSupportedE2eAlgorithm(roomEncryptionAlgorithm.name)
                    } else {
                        CanSendStatus.Allowed
                    }
                } else {
                    CanSendStatus.Allowed
                }
            } else {
                CanSendStatus.NoPermission
            }
        }.setOnEach {
            copy(canSendMessage = it)
        }
    }

    private fun handleEnterQuoteMode(action: MessageComposerAction.EnterQuoteMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Quote(timelineEvent, action.text)) }
        }
    }

    private fun handleEnterReplyMode(action: MessageComposerAction.EnterReplyMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Reply(timelineEvent, action.text)) }
        }
    }

    private fun handleSendMessage(action: MessageComposerAction.SendMessage) {
        withState { state ->
            analyticsTracker.capture(state.toAnalyticsComposer()).also {
                setState { copy(startsThread = false) }
            }
            when (state.sendMode) {
                is SendMode.Regular -> {
                    when (val slashCommandResult = commandParser.parseSlashCommand(
                            textMessage = action.text,
                            isInThreadTimeline = state.isInThreadTimeline())) {
                        is ParsedCommand.ErrorNotACommand                  -> {
                            // Send the text message to the room
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = action.text,
                                        autoMarkdown = action.autoMarkdown)
                            } else {
                                room.sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                            }

                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ErrorSyntax                       -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandError(slashCommandResult.command))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand            -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown("/"))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand          -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown(slashCommandResult.slashCommand))
                        }
                        is ParsedCommand.ErrorCommandNotSupportedInThreads -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotSupportedInThreads(slashCommandResult.command))
                        }
                        is ParsedCommand.SendPlainText                     -> {
                            // Send the text message to the room, without markdown
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = slashCommandResult.message,
                                        autoMarkdown = false)
                            } else {
                                room.sendTextMessage(slashCommandResult.message, autoMarkdown = false)
                            }
                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ChangeRoomName                    -> {
                            handleChangeRoomNameSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.Invite                            -> {
                            handleInviteSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.Invite3Pid                        -> {
                            handleInvite3pidSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.SetUserPowerLevel                 -> {
                            handleSetUserPowerLevel(slashCommandResult)
                        }
                        is ParsedCommand.ClearScalarToken                  -> {
                            // TODO
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown                       -> {
                            vectorPreferences.setMarkdownEnabled(slashCommandResult.enable)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(
                                    if (slashCommandResult.enable) R.string.markdown_has_been_enabled else R.string.markdown_has_been_disabled))
                            popDraft()
                        }
                        is ParsedCommand.BanUser                           -> {
                            handleBanSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.UnbanUser                         -> {
                            handleUnbanSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.IgnoreUser                        -> {
                            handleIgnoreSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.UnignoreUser                      -> {
                            handleUnignoreSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.RemoveUser                        -> {
                            handleRemoveSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.JoinRoom                          -> {
                            handleJoinToAnotherRoomSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.PartRoom                          -> {
                            handlePartSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.SendEmote                         -> {
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = slashCommandResult.message,
                                        msgType = MessageType.MSGTYPE_EMOTE,
                                        autoMarkdown = action.autoMarkdown)
                            } else {
                                room.sendTextMessage(slashCommandResult.message, msgType = MessageType.MSGTYPE_EMOTE, autoMarkdown = action.autoMarkdown)
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbow                       -> {
                            val message = slashCommandResult.message.toString()
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = slashCommandResult.message,
                                        formattedText = rainbowGenerator.generate(message))
                            } else {
                                room.sendFormattedTextMessage(message, rainbowGenerator.generate(message))
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbowEmote                  -> {
                            val message = slashCommandResult.message.toString()
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = slashCommandResult.message,
                                        msgType = MessageType.MSGTYPE_EMOTE,
                                        formattedText = rainbowGenerator.generate(message))
                            } else {
                                room.sendFormattedTextMessage(message, rainbowGenerator.generate(message), MessageType.MSGTYPE_EMOTE)
                            }

                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendSpoiler                       -> {
                            val text = "[${stringProvider.getString(R.string.spoiler)}](${slashCommandResult.message})"
                            val formattedText = "<span data-mx-spoiler>${slashCommandResult.message}</span>"
                            if (state.rootThreadEventId != null) {
                                room.replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = text,
                                        formattedText = formattedText)
                            } else {
                                room.sendFormattedTextMessage(
                                        text,
                                        formattedText)
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendShrug                         -> {
                            sendPrefixedMessage("¯\\_(ツ)_/¯", slashCommandResult.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendLenny                         -> {
                            sendPrefixedMessage("( ͡° ͜ʖ ͡°)", slashCommandResult.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendChatEffect                    -> {
                            sendChatEffect(slashCommandResult)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.ChangeTopic                       -> {
                            handleChangeTopicSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeDisplayName                 -> {
                            handleChangeDisplayNameSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeDisplayNameForRoom          -> {
                            handleChangeDisplayNameForRoomSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeRoomAvatar                  -> {
                            handleChangeRoomAvatarSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeAvatarForRoom               -> {
                            handleChangeAvatarForRoomSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ShowUser                          -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            handleWhoisSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.DiscardSession                    -> {
                            if (room.isEncrypted()) {
                                session.cryptoService().discardOutboundSession(room.roomId)
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                popDraft()
                            } else {
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                _viewEvents.post(
                                        MessageComposerViewEvents
                                                .ShowMessage(stringProvider.getString(R.string.command_description_discard_session_not_handled))
                                )
                            }
                        }
                        is ParsedCommand.CreateSpace                       -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val params = CreateSpaceParams().apply {
                                        name = slashCommandResult.name
                                        invitedUserIds.addAll(slashCommandResult.invitees)
                                    }
                                    val spaceId = session.spaceService().createSpace(params)
                                    session.spaceService().getSpace(spaceId)
                                            ?.addChildren(
                                                    state.roomId,
                                                    null,
                                                    null,
                                                    true
                                            )
                                    popDraft()
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.AddToSpace                        -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.spaceService().getSpace(slashCommandResult.spaceId)
                                            ?.addChildren(
                                                    room.roomId,
                                                    null,
                                                    null,
                                                    false
                                            )
                                    popDraft()
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.JoinSpace                         -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.spaceService().joinSpace(slashCommandResult.spaceIdOrAlias)
                                    popDraft()
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.LeaveRoom                         -> {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.leaveRoom(slashCommandResult.roomId)
                                    popDraft()
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.UpgradeRoom                       -> {
                            _viewEvents.post(
                                    MessageComposerViewEvents.ShowRoomUpgradeDialog(
                                            slashCommandResult.newVersion,
                                            room.roomSummary()?.isPublic ?: false
                                    )
                            )
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                    }
                }
                is SendMode.Edit    -> {
                    // is original event a reply?
                    val relationContent = state.sendMode.timelineEvent.getRelationContent()
                    val inReplyTo = if (state.rootThreadEventId != null) {
                        // Thread event
                        if (relationContent?.shouldRenderInThread() == true) {
                            // Reply within a thread event
                            relationContent.inReplyTo?.eventId
                        } else {
                            // Normal thread event
                            null
                        }
                    } else {
                        // Normal event
                        relationContent?.inReplyTo?.eventId
                    }

                    if (inReplyTo != null) {
                        // TODO check if same content?
                        room.getTimelineEvent(inReplyTo)?.let {
                            room.editReply(state.sendMode.timelineEvent, it, action.text.toString())
                        }
                    } else {
                        val messageContent = state.sendMode.timelineEvent.getLastMessageContent()
                        val existingBody = messageContent?.body ?: ""
                        if (existingBody != action.text) {
                            room.editTextMessage(state.sendMode.timelineEvent,
                                    messageContent?.msgType ?: MessageType.MSGTYPE_TEXT,
                                    action.text,
                                    action.autoMarkdown)
                        } else {
                            Timber.w("Same message content, do not send edition")
                        }
                    }
                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.Quote   -> {
                    room.sendQuotedTextMessage(
                            quotedEvent = state.sendMode.timelineEvent,
                            text = action.text.toString(),
                            autoMarkdown = action.autoMarkdown,
                            rootThreadEventId = state.rootThreadEventId)
                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.Reply   -> {
                    val timelineEvent = state.sendMode.timelineEvent
                    val showInThread = state.sendMode.timelineEvent.root.isThread() && state.rootThreadEventId == null
                    // If threads are disabled this will make the fallback replies visible to clients with threads enabled
                    val rootThreadEventId = if (showInThread) timelineEvent.root.getRootThreadEventId() else null
                    state.rootThreadEventId?.let {
                        room.replyInThread(
                                rootThreadEventId = it,
                                replyInThreadText = action.text.toString(),
                                autoMarkdown = action.autoMarkdown,
                                eventReplied = timelineEvent)
                    } ?: room.replyToMessage(
                            eventReplied = timelineEvent,
                            replyText = action.text.toString(),
                            autoMarkdown = action.autoMarkdown,
                            showInThread = showInThread,
                            rootThreadEventId = rootThreadEventId
                    )

                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.Voice   -> {
                    // do nothing
                }
            }
        }
    }

    private fun popDraft() = withState {
        if (it.sendMode is SendMode.Regular && it.sendMode.fromSharing) {
            // If we were sharing, we want to get back our last value from draft
            loadDraftIfAny()
        } else {
            // Otherwise we clear the composer and remove the draft from db
            setState { copy(sendMode = SendMode.Regular("", false)) }
            viewModelScope.launch {
                room.deleteDraft()
            }
        }
    }

    private fun loadDraftIfAny() {
        val currentDraft = room.getDraft()
        setState {
            copy(
                    // Create a sendMode from a draft and retrieve the TimelineEvent
                    sendMode = when (currentDraft) {
                        is UserDraft.Regular -> SendMode.Regular(currentDraft.content, false)
                        is UserDraft.Quote   -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Quote(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Reply   -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Reply(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Edit    -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Edit(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Voice   -> SendMode.Voice(currentDraft.content)
                        else                 -> null
                    } ?: SendMode.Regular("", fromSharing = false)
            )
        }
    }

    private fun handleUserIsTyping(action: MessageComposerAction.UserIsTyping) {
        if (vectorPreferences.sendTypingNotifs()) {
            if (action.isTyping) {
                room.userIsTyping()
            } else {
                room.userStopsTyping()
            }
        }
    }

    private fun sendChatEffect(sendChatEffect: ParsedCommand.SendChatEffect) {
        // If message is blank, convert to an emote, with default message
        if (sendChatEffect.message.isBlank()) {
            val defaultMessage = stringProvider.getString(when (sendChatEffect.chatEffect) {
                ChatEffect.CONFETTI -> R.string.default_message_emote_confetti
                ChatEffect.SNOWFALL -> R.string.default_message_emote_snow
            })
            room.sendTextMessage(defaultMessage, MessageType.MSGTYPE_EMOTE)
        } else {
            room.sendTextMessage(sendChatEffect.message, sendChatEffect.chatEffect.toMessageType())
        }
    }

    private fun handleJoinToAnotherRoomSlashCommand(command: ParsedCommand.JoinRoom) {
        viewModelScope.launch {
            try {
                session.joinRoom(command.roomAlias, command.reason, emptyList())
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                return@launch
            }
            session.getRoomSummary(command.roomAlias)
                    ?.also { analyticsTracker.capture(it.toAnalyticsJoinedRoom()) }
                    ?.roomId
                    ?.let {
                        _viewEvents.post(MessageComposerViewEvents.JoinRoomCommandSuccess(it))
                    }
        }
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
        launchSlashCommandFlowSuspendable {
            room.updateTopic(changeTopic.topic)
        }
    }

    private fun handleInviteSlashCommand(invite: ParsedCommand.Invite) {
        launchSlashCommandFlowSuspendable {
            room.invite(invite.userId, invite.reason)
        }
    }

    private fun handleInvite3pidSlashCommand(invite: ParsedCommand.Invite3Pid) {
        launchSlashCommandFlowSuspendable {
            room.invite3pid(invite.threePid)
        }
    }

    private fun handleSetUserPowerLevel(setUserPowerLevel: ParsedCommand.SetUserPowerLevel) {
        val newPowerLevelsContent = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?.setUserPowerLevel(setUserPowerLevel.userId, setUserPowerLevel.powerLevel)
                ?.toContent()
                ?: return

        launchSlashCommandFlowSuspendable {
            room.sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent)
        }
    }

    private fun handleChangeDisplayNameSlashCommand(changeDisplayName: ParsedCommand.ChangeDisplayName) {
        launchSlashCommandFlowSuspendable {
            session.setDisplayName(session.myUserId, changeDisplayName.displayName)
        }
    }

    private fun handlePartSlashCommand(command: ParsedCommand.PartRoom) {
        launchSlashCommandFlowSuspendable {
            if (command.roomAlias == null) {
                // Leave the current room
                room
            } else {
                session.getRoomSummary(roomIdOrAlias = command.roomAlias)
                        ?.roomId
                        ?.let { session.getRoom(it) }
            }
                    ?.let {
                        session.leaveRoom(it.roomId)
                    }
        }
    }

    private fun handleRemoveSlashCommand(removeUser: ParsedCommand.RemoveUser) {
        launchSlashCommandFlowSuspendable {
            room.remove(removeUser.userId, removeUser.reason)
        }
    }

    private fun handleBanSlashCommand(ban: ParsedCommand.BanUser) {
        launchSlashCommandFlowSuspendable {
            room.ban(ban.userId, ban.reason)
        }
    }

    private fun handleUnbanSlashCommand(unban: ParsedCommand.UnbanUser) {
        launchSlashCommandFlowSuspendable {
            room.unban(unban.userId, unban.reason)
        }
    }

    private fun handleChangeRoomNameSlashCommand(changeRoomName: ParsedCommand.ChangeRoomName) {
        launchSlashCommandFlowSuspendable {
            room.updateName(changeRoomName.name)
        }
    }

    private fun getMyRoomMemberContent(): RoomMemberContent? {
        return room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(session.myUserId))
                ?.content
                ?.toModel<RoomMemberContent>()
    }

    private fun handleChangeDisplayNameForRoomSlashCommand(changeDisplayName: ParsedCommand.ChangeDisplayNameForRoom) {
        launchSlashCommandFlowSuspendable {
            getMyRoomMemberContent()
                    ?.copy(displayName = changeDisplayName.displayName)
                    ?.toContent()
                    ?.let {
                        room.sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleChangeRoomAvatarSlashCommand(changeAvatar: ParsedCommand.ChangeRoomAvatar) {
        launchSlashCommandFlowSuspendable {
            room.sendStateEvent(EventType.STATE_ROOM_AVATAR, stateKey = "", RoomAvatarContent(changeAvatar.url).toContent())
        }
    }

    private fun handleChangeAvatarForRoomSlashCommand(changeAvatar: ParsedCommand.ChangeAvatarForRoom) {
        launchSlashCommandFlowSuspendable {
            getMyRoomMemberContent()
                    ?.copy(avatarUrl = changeAvatar.url)
                    ?.toContent()
                    ?.let {
                        room.sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleIgnoreSlashCommand(ignore: ParsedCommand.IgnoreUser) {
        launchSlashCommandFlowSuspendable {
            session.ignoreUserIds(listOf(ignore.userId))
        }
    }

    private fun handleUnignoreSlashCommand(unignore: ParsedCommand.UnignoreUser) {
        launchSlashCommandFlowSuspendable {
            session.unIgnoreUserIds(listOf(unignore.userId))
        }
    }

    private fun handleWhoisSlashCommand(whois: ParsedCommand.ShowUser) {
        _viewEvents.post(MessageComposerViewEvents.OpenRoomMemberProfile(whois.userId))
    }

    private fun sendPrefixedMessage(prefix: String, message: CharSequence, rootThreadEventId: String?) {
        val sequence = buildString {
            append(prefix)
            if (message.isNotEmpty()) {
                append(" ")
                append(message)
            }
        }
        rootThreadEventId?.let {
            room.replyInThread(it, sequence)
        } ?: room.sendTextMessage(sequence)
    }

    /**
     * Convert a send mode to a draft and save the draft
     */
    private fun handleSaveTextDraft(draft: String) = withState {
        session.coroutineScope.launch {
            when {
                it.sendMode is SendMode.Regular && !it.sendMode.fromSharing -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.saveDraft(UserDraft.Regular(draft))
                }
                it.sendMode is SendMode.Reply                               -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.saveDraft(UserDraft.Reply(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
                it.sendMode is SendMode.Quote                               -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.saveDraft(UserDraft.Quote(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
                it.sendMode is SendMode.Edit                                -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.saveDraft(UserDraft.Edit(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
            }
        }
    }

    private fun handleStartRecordingVoiceMessage() {
        try {
            audioMessageHelper.startRecording(room.roomId)
        } catch (failure: Throwable) {
            _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
        }
    }

    private fun handleEndRecordingVoiceMessage(isCancelled: Boolean, rootThreadEventId: String? = null) {
        audioMessageHelper.stopPlayback()
        if (isCancelled) {
            audioMessageHelper.deleteRecording()
        } else {
            audioMessageHelper.stopRecording(convertForSending = true)?.let { audioType ->
                if (audioType.duration > 1000) {
                    room.sendMedia(
                            attachment = audioType.toContentAttachmentData(isVoiceMessage = true),
                            compressBeforeSending = false,
                            roomIds = emptySet(),
                            rootThreadEventId = rootThreadEventId)
                } else {
                    audioMessageHelper.deleteRecording()
                }
            }
        }
        handleEnterRegularMode(MessageComposerAction.EnterRegularMode(text = "", fromSharing = false))
    }

    private fun handlePlayOrPauseVoicePlayback(action: MessageComposerAction.PlayOrPauseVoicePlayback) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Download can fail
                val audioFile = session.fileService().downloadFile(action.messageAudioContent)
                // Conversion can fail, fallback to the original file in this case and let the player fail for us
                val convertedFile = voicePlayerHelper.convertFile(audioFile) ?: audioFile
                // Play can fail
                audioMessageHelper.startOrPausePlayback(action.eventId, convertedFile)
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
            }
        }
    }

    private fun handlePlayOrPauseRecordingPlayback() {
        audioMessageHelper.startOrPauseRecordingPlayback()
    }

    private fun handleEndAllVoiceActions(deleteRecord: Boolean) {
        audioMessageHelper.clearTracker()
        audioMessageHelper.stopAllVoiceActions(deleteRecord)
    }

    private fun handleInitializeVoiceRecorder(attachmentData: ContentAttachmentData) {
        audioMessageHelper.initializeRecorder(attachmentData)
        setState { copy(voiceRecordingUiState = VoiceMessageRecorderView.RecordingUiState.Draft) }
    }

    private fun handlePauseRecordingVoiceMessage() {
        audioMessageHelper.pauseRecording()
    }

    private fun handleVoiceWaveformTouchedUp(action: MessageComposerAction.VoiceWaveformTouchedUp) {
        audioMessageHelper.movePlaybackTo(action.eventId, action.percentage, action.duration)
    }

    private fun handleVoiceWaveformMovedTo(action: MessageComposerAction.VoiceWaveformMovedTo) {
        audioMessageHelper.movePlaybackTo(action.eventId, action.percentage, action.duration)
    }

    private fun handleAudioSeekBarMovedTo(action: MessageComposerAction.AudioSeekBarMovedTo) {
        audioMessageHelper.movePlaybackTo(action.eventId, action.percentage, action.duration)
    }

    private fun handleEntersBackground(composerText: String) {
        // Always stop all voice actions. It may be playing in timeline or active recording
        val playingAudioContent = audioMessageHelper.stopAllVoiceActions(deleteRecord = false)

        val isVoiceRecording = com.airbnb.mvrx.withState(this) { it.isVoiceRecording }
        if (isVoiceRecording) {
            viewModelScope.launch {
                playingAudioContent?.toContentAttachmentData()?.let { voiceDraft ->
                    val content = voiceDraft.toJsonString()
                    room.saveDraft(UserDraft.Voice(content))
                    setState { copy(sendMode = SendMode.Voice(content)) }
                }
            }
        } else {
            handleSaveTextDraft(draft = composerText)
        }
    }

    private fun launchSlashCommandFlowSuspendable(block: suspend () -> Unit) {
        _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
        viewModelScope.launch {
            val event = try {
                block()
                popDraft()
                MessageComposerViewEvents.SlashCommandResultOk()
            } catch (failure: Throwable) {
                MessageComposerViewEvents.SlashCommandResultError(failure)
            }
            _viewEvents.post(event)
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<MessageComposerViewModel, MessageComposerViewState> {
        override fun create(initialState: MessageComposerViewState): MessageComposerViewModel
    }

    companion object : MavericksViewModelFactory<MessageComposerViewModel, MessageComposerViewState> by hiltMavericksViewModelFactory()
}
