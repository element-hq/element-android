/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import android.text.SpannableString
import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.withState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsComposer
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.analytics.plan.JoinedRoom
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
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.VoiceBroadcastHelper
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventLiveUseCase
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomEncryptionAlgorithm
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContentWithFormattedBody
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.shouldRenderInThread
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.timeline.getRelationContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import org.matrix.android.sdk.api.util.Optional
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
        private val voiceBroadcastHelper: VoiceBroadcastHelper,
        private val clock: Clock,
        private val getVoiceBroadcastStateEventLiveUseCase: GetVoiceBroadcastStateEventLiveUseCase,
) : VectorViewModel<MessageComposerViewState, MessageComposerAction, MessageComposerViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)

    // Keep it out of state to avoid invalidate being called
    private var currentComposerText: CharSequence = ""

    init {
        if (room != null) {
            loadDraftIfAny(room)
            observePowerLevelAndEncryption(room)
            observeVoiceBroadcast(room)
            subscribeToStateInternal()
        } else {
            onRoomError()
        }
    }

    override fun handle(action: MessageComposerAction) {
        val room = this.room ?: return
        when (action) {
            is MessageComposerAction.EnterEditMode -> handleEnterEditMode(room, action)
            is MessageComposerAction.EnterQuoteMode -> handleEnterQuoteMode(room, action)
            is MessageComposerAction.EnterRegularMode -> handleEnterRegularMode(action)
            is MessageComposerAction.EnterReplyMode -> handleEnterReplyMode(room, action)
            is MessageComposerAction.SendMessage -> handleSendMessage(room, action)
            is MessageComposerAction.UserIsTyping -> handleUserIsTyping(room, action)
            is MessageComposerAction.OnTextChanged -> handleOnTextChanged(action)
            is MessageComposerAction.OnVoiceRecordingUiStateChanged -> handleOnVoiceRecordingUiStateChanged(action)
            is MessageComposerAction.StartRecordingVoiceMessage -> handleStartRecordingVoiceMessage(room)
            is MessageComposerAction.EndRecordingVoiceMessage -> handleEndRecordingVoiceMessage(room, action.isCancelled, action.rootThreadEventId)
            is MessageComposerAction.PlayOrPauseVoicePlayback -> handlePlayOrPauseVoicePlayback(action)
            MessageComposerAction.PauseRecordingVoiceMessage -> handlePauseRecordingVoiceMessage()
            MessageComposerAction.PlayOrPauseRecordingPlayback -> handlePlayOrPauseRecordingPlayback()
            is MessageComposerAction.InitializeVoiceRecorder -> handleInitializeVoiceRecorder(room, action.attachmentData)
            is MessageComposerAction.OnEntersBackground -> handleEntersBackground(room, action.composerText)
            is MessageComposerAction.VoiceWaveformTouchedUp -> handleVoiceWaveformTouchedUp(action)
            is MessageComposerAction.VoiceWaveformMovedTo -> handleVoiceWaveformMovedTo(action)
            is MessageComposerAction.AudioSeekBarMovedTo -> handleAudioSeekBarMovedTo(action)
            is MessageComposerAction.SlashCommandConfirmed -> handleSlashCommandConfirmed(room, action)
            is MessageComposerAction.InsertUserDisplayName -> handleInsertUserDisplayName(action)
            is MessageComposerAction.SetFullScreen -> handleSetFullScreen(action)
        }
    }

    private fun handleOnVoiceRecordingUiStateChanged(action: MessageComposerAction.OnVoiceRecordingUiStateChanged) = setState {
        copy(voiceRecordingUiState = action.uiState)
    }

    private fun handleOnTextChanged(action: MessageComposerAction.OnTextChanged) {
        val needsSendButtonVisibilityUpdate = currentComposerText.isBlank() != action.text.isBlank()
        currentComposerText = SpannableString(action.text)
        if (needsSendButtonVisibilityUpdate) {
            updateIsSendButtonVisibility(true)
        }
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
        copy(sendMode = SendMode.Regular(currentComposerText, action.fromSharing))
    }

    private fun handleEnterEditMode(room: Room, action: MessageComposerAction.EnterEditMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            val formatted = vectorPreferences.isRichTextEditorEnabled()
            val editableContent = timelineEvent.getTextEditableContent(formatted)
            setState { copy(sendMode = SendMode.Edit(timelineEvent, editableContent)) }
        }
    }

    private fun handleSetFullScreen(action: MessageComposerAction.SetFullScreen) {
        setState { copy(isFullScreen = action.isFullScreen) }
    }

    private fun observePowerLevelAndEncryption(room: Room) {
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

    private fun observeVoiceBroadcast(room: Room) {
        room.stateService().getStateEventLive(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO, QueryStringValue.Equals(session.myUserId))
                .asFlow()
                .map { it.getOrNull()?.asVoiceBroadcastEvent()?.voiceBroadcastId }
                .flatMapLatest { voiceBroadcastId ->
                    voiceBroadcastId?.let { getVoiceBroadcastStateEventLiveUseCase.execute(VoiceBroadcast(it, room.roomId)) } ?: flowOf(Optional.empty())
                }
                .map { it.getOrNull()?.content?.voiceBroadcastState }
                .setOnEach {
                    copy(voiceBroadcastState = it)
                }
    }

    private fun handleEnterQuoteMode(room: Room, action: MessageComposerAction.EnterQuoteMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Quote(timelineEvent, currentComposerText)) }
        }
    }

    private fun handleEnterReplyMode(room: Room, action: MessageComposerAction.EnterReplyMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Reply(timelineEvent, currentComposerText)) }
        }
    }

    private fun handleSendMessage(room: Room, action: MessageComposerAction.SendMessage) {
        withState { state ->
            analyticsTracker.capture(state.toAnalyticsComposer())
            setState { copy(startsThread = false) }
            when (state.sendMode) {
                is SendMode.Regular -> {
                    when (val parsedCommand = commandParser.parseSlashCommand(
                            textMessage = action.text,
                            formattedMessage = action.formattedText,
                            isInThreadTimeline = state.isInThreadTimeline()
                    )) {
                        is ParsedCommand.ErrorNotACommand -> {
                            // Send the text message to the room
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = action.text,
                                        formattedText = action.formattedText,
                                        autoMarkdown = action.autoMarkdown
                                )
                            } else {
                                if (action.formattedText != null) {
                                    room.sendService().sendFormattedTextMessage(action.text.toString(), action.formattedText)
                                } else {
                                    room.sendService().sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                                }
                            }

                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft(room)
                        }
                        is ParsedCommand.ErrorSyntax -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandError(parsedCommand.command))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown("/"))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown(parsedCommand.slashCommand))
                        }
                        is ParsedCommand.ErrorCommandNotSupportedInThreads -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotSupportedInThreads(parsedCommand.command))
                        }
                        is ParsedCommand.SendPlainText -> {
                            // Send the text message to the room, without markdown
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = parsedCommand.message,
                                        autoMarkdown = false
                                )
                            } else {
                                room.sendService().sendTextMessage(parsedCommand.message, autoMarkdown = false)
                            }
                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft(room)
                        }
                        is ParsedCommand.SendFormattedText -> {
                            // Send the text message to the room, without markdown
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = parsedCommand.message,
                                        formattedText = parsedCommand.formattedMessage,
                                        autoMarkdown = false
                                )
                            } else {
                                room.sendService().sendFormattedTextMessage(
                                        text = parsedCommand.message.toString(),
                                        formattedText = parsedCommand.formattedMessage
                                )
                            }
                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft(room)
                        }
                        is ParsedCommand.ChangeRoomName -> {
                            handleChangeRoomNameSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.Invite -> {
                            handleInviteSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.Invite3Pid -> {
                            handleInvite3pidSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.SetUserPowerLevel -> {
                            handleSetUserPowerLevel(room, parsedCommand)
                        }
                        is ParsedCommand.DevTools -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.ClearScalarToken -> {
                            // TODO
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown -> {
                            vectorPreferences.setMarkdownEnabled(parsedCommand.enable)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.BanUser -> {
                            handleBanSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.UnbanUser -> {
                            handleUnbanSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.IgnoreUser -> {
                            handleIgnoreSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.UnignoreUser -> {
                            handleUnignoreSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.RemoveUser -> {
                            handleRemoveSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.JoinRoom -> {
                            handleJoinToAnotherRoomSlashCommand(parsedCommand)
                            popDraft(room)
                        }
                        is ParsedCommand.PartRoom -> {
                            handlePartSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.SendEmote -> {
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = parsedCommand.message,
                                        msgType = MessageType.MSGTYPE_EMOTE,
                                        autoMarkdown = action.autoMarkdown
                                )
                            } else {
                                room.sendService().sendTextMessage(
                                        text = parsedCommand.message,
                                        msgType = MessageType.MSGTYPE_EMOTE,
                                        autoMarkdown = action.autoMarkdown
                                )
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendRainbow -> {
                            val message = parsedCommand.message.toString()
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = parsedCommand.message,
                                        formattedText = rainbowGenerator.generate(message)
                                )
                            } else {
                                room.sendService().sendFormattedTextMessage(message, rainbowGenerator.generate(message))
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendRainbowEmote -> {
                            val message = parsedCommand.message.toString()
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = parsedCommand.message,
                                        msgType = MessageType.MSGTYPE_EMOTE,
                                        formattedText = rainbowGenerator.generate(message)
                                )
                            } else {
                                room.sendService().sendFormattedTextMessage(message, rainbowGenerator.generate(message), MessageType.MSGTYPE_EMOTE)
                            }

                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendSpoiler -> {
                            val text = "[${stringProvider.getString(CommonStrings.spoiler)}](${parsedCommand.message})"
                            val formattedText = "<span data-mx-spoiler>${parsedCommand.message}</span>"
                            if (state.rootThreadEventId != null) {
                                room.relationService().replyInThread(
                                        rootThreadEventId = state.rootThreadEventId,
                                        replyInThreadText = text,
                                        formattedText = formattedText
                                )
                            } else {
                                room.sendService().sendFormattedTextMessage(
                                        text,
                                        formattedText
                                )
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendShrug -> {
                            sendPrefixedMessage(room, "¯\\_(ツ)_/¯", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendLenny -> {
                            sendPrefixedMessage(room, "( ͡° ͜ʖ ͡°)", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendTableFlip -> {
                            sendPrefixedMessage(room, "(╯°□°）╯︵ ┻━┻", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.SendChatEffect -> {
                            sendChatEffect(room, parsedCommand)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                        is ParsedCommand.ChangeTopic -> {
                            handleChangeTopicSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.ChangeDisplayName -> {
                            handleChangeDisplayNameSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.ChangeDisplayNameForRoom -> {
                            handleChangeDisplayNameForRoomSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.ChangeRoomAvatar -> {
                            handleChangeRoomAvatarSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.ChangeAvatarForRoom -> {
                            handleChangeAvatarForRoomSlashCommand(room, parsedCommand)
                        }
                        is ParsedCommand.ShowUser -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            handleWhoisSlashCommand(parsedCommand)
                            popDraft(room)
                        }
                        is ParsedCommand.DiscardSession -> {
                            if (room.roomCryptoService().isEncrypted()) {
                                session.cryptoService().discardOutboundSession(room.roomId)
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                popDraft(room)
                            } else {
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                _viewEvents.post(
                                        MessageComposerViewEvents
                                                .ShowMessage(stringProvider.getString(CommonStrings.command_description_discard_session_not_handled))
                                )
                            }
                        }
                        is ParsedCommand.CreateSpace -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val params = CreateSpaceParams().apply {
                                        name = parsedCommand.name
                                        invitedUserIds.addAll(parsedCommand.invitees)
                                    }
                                    val spaceId = session.spaceService().createSpace(params)
                                    session.spaceService().getSpace(spaceId)
                                            ?.addChildren(
                                                    state.roomId,
                                                    null,
                                                    null,
                                                    true
                                            )
                                    popDraft(room)
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.AddToSpace -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.spaceService().getSpace(parsedCommand.spaceId)
                                            ?.addChildren(
                                                    room.roomId,
                                                    null,
                                                    null,
                                                    false
                                            )
                                    popDraft(room)
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.JoinSpace -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.spaceService().joinSpace(parsedCommand.spaceIdOrAlias)
                                    popDraft(room)
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.LeaveRoom -> {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.roomService().leaveRoom(parsedCommand.roomId)
                                    popDraft(room)
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.UpgradeRoom -> {
                            _viewEvents.post(
                                    MessageComposerViewEvents.ShowRoomUpgradeDialog(
                                            parsedCommand.newVersion,
                                            room.roomSummary()?.isPublic ?: false
                                    )
                            )
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft(room)
                        }
                    }
                }
                is SendMode.Edit -> {
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
                            room.relationService().editReply(state.sendMode.timelineEvent, it, action.text, action.formattedText)
                        }
                    } else {
                        val messageContent = state.sendMode.timelineEvent.getVectorLastMessageContent()
                        val existingBody: String
                        val needsEdit = if (messageContent is MessageContentWithFormattedBody) {
                            existingBody = messageContent.formattedBody ?: ""
                            existingBody != action.formattedText
                        } else {
                            existingBody = messageContent?.body ?: ""
                            existingBody != action.text
                        }
                        if (needsEdit) {
                            room.relationService().editTextMessage(
                                    state.sendMode.timelineEvent,
                                    messageContent?.msgType ?: MessageType.MSGTYPE_TEXT,
                                    action.text,
                                    action.formattedText,
                                    action.autoMarkdown
                            )
                        } else {
                            Timber.w("Same message content, do not send edition")
                        }
                    }
                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft(room)
                }
                is SendMode.Quote -> {
                    room.sendService().sendQuotedTextMessage(
                            quotedEvent = state.sendMode.timelineEvent,
                            text = action.text.toString(),
                            formattedText = action.formattedText,
                            autoMarkdown = action.autoMarkdown,
                            rootThreadEventId = state.rootThreadEventId
                    )
                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft(room)
                }
                is SendMode.Reply -> {
                    val timelineEvent = state.sendMode.timelineEvent
                    val showInThread = state.sendMode.timelineEvent.root.isThread() && state.rootThreadEventId == null
                    // If threads are disabled this will make the fallback replies visible to clients with threads enabled
                    val rootThreadEventId = if (showInThread) timelineEvent.root.getRootThreadEventId() else null
                    state.rootThreadEventId?.let {
                        room.relationService().replyInThread(
                                rootThreadEventId = it,
                                replyInThreadText = action.text,
                                autoMarkdown = action.autoMarkdown,
                                formattedText = action.formattedText,
                                eventReplied = timelineEvent
                        )
                    } ?: room.relationService().replyToMessage(
                            eventReplied = timelineEvent,
                            replyText = action.text,
                            replyFormattedText = action.formattedText,
                            autoMarkdown = action.autoMarkdown,
                            showInThread = showInThread,
                            rootThreadEventId = rootThreadEventId
                    )

                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft(room)
                }
                is SendMode.Voice -> {
                    // do nothing
                }
            }
        }
    }

    private fun popDraft(room: Room) = withState {
        if (it.sendMode is SendMode.Regular && it.sendMode.fromSharing) {
            // If we were sharing, we want to get back our last value from draft
            loadDraftIfAny(room)
        } else {
            // Otherwise we clear the composer and remove the draft from db
            setState { copy(sendMode = SendMode.Regular("", false)) }
            viewModelScope.launch {
                room.draftService().deleteDraft()
            }
        }
    }

    private fun loadDraftIfAny(room: Room) {
        val currentDraft = room.draftService().getDraft()
        setState {
            copy(
                    // Create a sendMode from a draft and retrieve the TimelineEvent
                    sendMode = when (currentDraft) {
                        is UserDraft.Regular -> SendMode.Regular(currentDraft.content, false)
                        is UserDraft.Quote -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Quote(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Reply -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Reply(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Edit -> {
                            room.getTimelineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Edit(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Voice -> SendMode.Voice(currentDraft.content)
                        else -> null
                    } ?: SendMode.Regular("", fromSharing = false)
            )
        }
    }

    private fun handleUserIsTyping(room: Room, action: MessageComposerAction.UserIsTyping) {
        if (vectorPreferences.sendTypingNotifs()) {
            if (action.isTyping) {
                room.typingService().userIsTyping()
            } else {
                room.typingService().userStopsTyping()
            }
        }
    }

    private fun sendChatEffect(room: Room, sendChatEffect: ParsedCommand.SendChatEffect) {
        // If message is blank, convert to an emote, with default message
        if (sendChatEffect.message.isBlank()) {
            val defaultMessage = stringProvider.getString(
                    when (sendChatEffect.chatEffect) {
                        ChatEffect.CONFETTI -> CommonStrings.default_message_emote_confetti
                        ChatEffect.SNOWFALL -> CommonStrings.default_message_emote_snow
                    }
            )
            room.sendService().sendTextMessage(defaultMessage, MessageType.MSGTYPE_EMOTE)
        } else {
            room.sendService().sendTextMessage(sendChatEffect.message, sendChatEffect.chatEffect.toMessageType())
        }
    }

    private fun handleJoinToAnotherRoomSlashCommand(command: ParsedCommand.JoinRoom) {
        viewModelScope.launch {
            try {
                session.roomService().joinRoom(command.roomAlias, command.reason, emptyList())
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                return@launch
            }
            session.getRoomSummary(command.roomAlias)
                    ?.also { analyticsTracker.capture(it.toAnalyticsJoinedRoom(JoinedRoom.Trigger.SlashCommand)) }
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

    private fun handleChangeTopicSlashCommand(room: Room, changeTopic: ParsedCommand.ChangeTopic) {
        launchSlashCommandFlowSuspendable(room, changeTopic) {
            room.stateService().updateTopic(changeTopic.topic)
        }
    }

    private fun handleInviteSlashCommand(room: Room, invite: ParsedCommand.Invite) {
        launchSlashCommandFlowSuspendable(room, invite) {
            room.membershipService().invite(invite.userId, invite.reason)
        }
    }

    private fun handleInvite3pidSlashCommand(room: Room, invite: ParsedCommand.Invite3Pid) {
        launchSlashCommandFlowSuspendable(room, invite) {
            room.membershipService().invite3pid(invite.threePid)
        }
    }

    private fun handleSetUserPowerLevel(room: Room, setUserPowerLevel: ParsedCommand.SetUserPowerLevel) {
        val newPowerLevelsContent = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?.setUserPowerLevel(setUserPowerLevel.userId, setUserPowerLevel.powerLevel)
                ?.toContent()
                ?: return

        launchSlashCommandFlowSuspendable(room, setUserPowerLevel) {
            room.stateService().sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent)
        }
    }

    private fun handleChangeDisplayNameSlashCommand(room: Room, changeDisplayName: ParsedCommand.ChangeDisplayName) {
        launchSlashCommandFlowSuspendable(room, changeDisplayName) {
            session.profileService().setDisplayName(session.myUserId, changeDisplayName.displayName)
        }
    }

    private fun handlePartSlashCommand(room: Room, command: ParsedCommand.PartRoom) {
        launchSlashCommandFlowSuspendable(room, command) {
            if (command.roomAlias == null) {
                // Leave the current room
                room
            } else {
                session.getRoomSummary(roomIdOrAlias = command.roomAlias)
                        ?.roomId
                        ?.let { session.getRoom(it) }
            }
                    ?.let {
                        session.roomService().leaveRoom(it.roomId)
                    }
        }
    }

    private fun handleRemoveSlashCommand(room: Room, removeUser: ParsedCommand.RemoveUser) {
        launchSlashCommandFlowSuspendable(room, removeUser) {
            room.membershipService().remove(removeUser.userId, removeUser.reason)
        }
    }

    private fun handleBanSlashCommand(room: Room, ban: ParsedCommand.BanUser) {
        launchSlashCommandFlowSuspendable(room, ban) {
            room.membershipService().ban(ban.userId, ban.reason)
        }
    }

    private fun handleUnbanSlashCommand(room: Room, unban: ParsedCommand.UnbanUser) {
        launchSlashCommandFlowSuspendable(room, unban) {
            room.membershipService().unban(unban.userId, unban.reason)
        }
    }

    private fun handleChangeRoomNameSlashCommand(room: Room, changeRoomName: ParsedCommand.ChangeRoomName) {
        launchSlashCommandFlowSuspendable(room, changeRoomName) {
            room.stateService().updateName(changeRoomName.name)
        }
    }

    private fun getMyRoomMemberContent(room: Room): RoomMemberContent? {
        return room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(session.myUserId))
                ?.content
                ?.toModel<RoomMemberContent>()
    }

    private fun handleChangeDisplayNameForRoomSlashCommand(room: Room, changeDisplayName: ParsedCommand.ChangeDisplayNameForRoom) {
        launchSlashCommandFlowSuspendable(room, changeDisplayName) {
            getMyRoomMemberContent(room)
                    ?.copy(displayName = changeDisplayName.displayName)
                    ?.toContent()
                    ?.let {
                        room.stateService().sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleChangeRoomAvatarSlashCommand(room: Room, changeAvatar: ParsedCommand.ChangeRoomAvatar) {
        launchSlashCommandFlowSuspendable(room, changeAvatar) {
            room.stateService().sendStateEvent(EventType.STATE_ROOM_AVATAR, stateKey = "", RoomAvatarContent(changeAvatar.url).toContent())
        }
    }

    private fun handleChangeAvatarForRoomSlashCommand(room: Room, changeAvatar: ParsedCommand.ChangeAvatarForRoom) {
        launchSlashCommandFlowSuspendable(room, changeAvatar) {
            getMyRoomMemberContent(room)
                    ?.copy(avatarUrl = changeAvatar.url)
                    ?.toContent()
                    ?.let {
                        room.stateService().sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleIgnoreSlashCommand(room: Room, ignore: ParsedCommand.IgnoreUser) {
        launchSlashCommandFlowSuspendable(room, ignore) {
            session.userService().ignoreUserIds(listOf(ignore.userId))
        }
    }

    private fun handleUnignoreSlashCommand(unignore: ParsedCommand.UnignoreUser) {
        _viewEvents.post(MessageComposerViewEvents.SlashCommandConfirmationRequest(unignore))
    }

    private fun handleSlashCommandConfirmed(room: Room, action: MessageComposerAction.SlashCommandConfirmed) {
        when (action.parsedCommand) {
            is ParsedCommand.UnignoreUser -> handleUnignoreSlashCommandConfirmed(room, action.parsedCommand)
            else -> TODO("Not handled yet")
        }
    }

    private fun handleUnignoreSlashCommandConfirmed(room: Room, unignore: ParsedCommand.UnignoreUser) {
        launchSlashCommandFlowSuspendable(room, unignore) {
            session.userService().unIgnoreUserIds(listOf(unignore.userId))
        }
    }

    private fun handleWhoisSlashCommand(whois: ParsedCommand.ShowUser) {
        _viewEvents.post(MessageComposerViewEvents.OpenRoomMemberProfile(whois.userId))
    }

    private fun sendPrefixedMessage(room: Room, prefix: String, message: CharSequence, rootThreadEventId: String?) {
        val sequence = buildString {
            append(prefix)
            if (message.isNotEmpty()) {
                append(" ")
                append(message)
            }
        }
        rootThreadEventId?.let {
            room.relationService().replyInThread(it, sequence)
        } ?: room.sendService().sendTextMessage(sequence)
    }

    /**
     * Convert a send mode to a draft and save the draft.
     */
    private fun handleSaveTextDraft(room: Room, draft: String) = withState {
        session.coroutineScope.launch {
            when {
                it.sendMode is SendMode.Regular && !it.sendMode.fromSharing -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.draftService().saveDraft(UserDraft.Regular(draft))
                }
                it.sendMode is SendMode.Reply -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.draftService().saveDraft(UserDraft.Reply(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
                it.sendMode is SendMode.Quote -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.draftService().saveDraft(UserDraft.Quote(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
                it.sendMode is SendMode.Edit -> {
                    setState { copy(sendMode = it.sendMode.copy(text = draft)) }
                    room.draftService().saveDraft(UserDraft.Edit(it.sendMode.timelineEvent.root.eventId!!, draft))
                }
            }
        }
    }

    private fun handleStartRecordingVoiceMessage(room: Room) {
        val voiceBroadcastState = withState(this) { it.voiceBroadcastState }
        if (voiceBroadcastState != null && voiceBroadcastState != VoiceBroadcastState.STOPPED) {
            _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(VoiceFailure.VoiceBroadcastInProgress))
        } else {
            try {
                audioMessageHelper.startRecording(room.roomId)
                setState { copy(voiceRecordingUiState = VoiceMessageRecorderView.RecordingUiState.Recording(clock.epochMillis())) }
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
            }
        }
    }

    private fun handleEndRecordingVoiceMessage(room: Room, isCancelled: Boolean, rootThreadEventId: String? = null) {
        audioMessageHelper.stopPlayback()
        if (isCancelled) {
            audioMessageHelper.deleteRecording()
        } else {
            audioMessageHelper.stopRecording()?.let { audioType ->
                if (audioType.duration > 1000) {
                    room.sendService().sendMedia(
                            attachment = audioType.toContentAttachmentData(isVoiceMessage = true),
                            compressBeforeSending = false,
                            roomIds = emptySet(),
                            rootThreadEventId = rootThreadEventId
                    )
                } else {
                    audioMessageHelper.deleteRecording()
                }
            }
        }
        handleEnterRegularMode(MessageComposerAction.EnterRegularMode(fromSharing = false))
    }

    private fun handlePlayOrPauseVoicePlayback(action: MessageComposerAction.PlayOrPauseVoicePlayback) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Download can fail
                val audioFile = session.fileService().downloadFile(action.messageAudioContent)
                // Play can fail
                audioMessageHelper.startOrPausePlayback(action.eventId, audioFile)
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
            }
        }
    }

    private fun handlePlayOrPauseRecordingPlayback() {
        try {
            audioMessageHelper.startOrPauseRecordingPlayback()
        } catch (failure: Throwable) {
            _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
        }
    }

    fun endAllVoiceActions(deleteRecord: Boolean = true) {
        audioMessageHelper.stopTracking()
        audioMessageHelper.stopAllVoiceActions(deleteRecord)
    }

    private fun handleInitializeVoiceRecorder(room: Room, attachmentData: ContentAttachmentData) {
        audioMessageHelper.initializeRecorder(room.roomId, attachmentData)
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

    private fun handleEntersBackground(room: Room, composerText: String) {
        // Always stop all voice actions. It may be playing in timeline or active recording
        val playingAudioContent = audioMessageHelper.stopAllVoiceActions(deleteRecord = false)
        // TODO remove this when there will be a listening indicator outside of the timeline
        voiceBroadcastHelper.pausePlayback()

        val isVoiceRecording = com.airbnb.mvrx.withState(this) { it.isVoiceRecording }
        if (isVoiceRecording) {
            viewModelScope.launch {
                playingAudioContent?.toContentAttachmentData()?.let { voiceDraft ->
                    val content = voiceDraft.toJsonString()
                    room.draftService().saveDraft(UserDraft.Voice(content))
                    setState { copy(sendMode = SendMode.Voice(content)) }
                }
            }
        } else {
            handleSaveTextDraft(room = room, draft = composerText)
        }
    }

    private fun handleInsertUserDisplayName(action: MessageComposerAction.InsertUserDisplayName) {
        _viewEvents.post(MessageComposerViewEvents.InsertUserDisplayName(action.userId))
    }

    private fun launchSlashCommandFlowSuspendable(room: Room, parsedCommand: ParsedCommand, block: suspend () -> Unit) {
        _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
        viewModelScope.launch {
            val event = try {
                block()
                popDraft(room)
                MessageComposerViewEvents.SlashCommandResultOk(parsedCommand)
            } catch (failure: Throwable) {
                MessageComposerViewEvents.SlashCommandResultError(failure)
            }
            _viewEvents.post(event)
        }
    }

    private fun onRoomError() = setState {
        copy(isRoomError = true)
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<MessageComposerViewModel, MessageComposerViewState> {
        override fun create(initialState: MessageComposerViewState): MessageComposerViewModel
    }

    companion object : MavericksViewModelFactory<MessageComposerViewModel, MessageComposerViewState> by hiltMavericksViewModelFactory()
}
