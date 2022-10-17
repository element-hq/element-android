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

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
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
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.VoiceBroadcastHelper
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
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
) : VectorViewModel<MessageComposerViewState, MessageComposerAction, MessageComposerViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    // Keep it out of state to avoid invalidate being called
    private var currentComposerText: CharSequence = ""

    init {
        loadDraftIfAny()
        observePowerLevelAndEncryption()
        observeVoiceBroadcast()
        subscribeToStateInternal()
    }

    override fun handle(action: MessageComposerAction) {
        when (action) {
            is MessageComposerAction.EnterEditMode -> handleEnterEditMode(action)
            is MessageComposerAction.EnterQuoteMode -> handleEnterQuoteMode(action)
            is MessageComposerAction.EnterRegularMode -> handleEnterRegularMode(action)
            is MessageComposerAction.EnterReplyMode -> handleEnterReplyMode(action)
            is MessageComposerAction.SendMessage -> handleSendMessage(action)
            is MessageComposerAction.UserIsTyping -> handleUserIsTyping(action)
            is MessageComposerAction.OnTextChanged -> handleOnTextChanged(action)
            is MessageComposerAction.OnVoiceRecordingUiStateChanged -> handleOnVoiceRecordingUiStateChanged(action)
            is MessageComposerAction.StartRecordingVoiceMessage -> handleStartRecordingVoiceMessage()
            is MessageComposerAction.EndRecordingVoiceMessage -> handleEndRecordingVoiceMessage(action.isCancelled, action.rootThreadEventId)
            is MessageComposerAction.PlayOrPauseVoicePlayback -> handlePlayOrPauseVoicePlayback(action)
            MessageComposerAction.PauseRecordingVoiceMessage -> handlePauseRecordingVoiceMessage()
            MessageComposerAction.PlayOrPauseRecordingPlayback -> handlePlayOrPauseRecordingPlayback()
            is MessageComposerAction.InitializeVoiceRecorder -> handleInitializeVoiceRecorder(action.attachmentData)
            is MessageComposerAction.OnEntersBackground -> handleEntersBackground(action.composerText)
            is MessageComposerAction.VoiceWaveformTouchedUp -> handleVoiceWaveformTouchedUp(action)
            is MessageComposerAction.VoiceWaveformMovedTo -> handleVoiceWaveformMovedTo(action)
            is MessageComposerAction.AudioSeekBarMovedTo -> handleAudioSeekBarMovedTo(action)
            is MessageComposerAction.SlashCommandConfirmed -> handleSlashCommandConfirmed(action)
            is MessageComposerAction.InsertUserDisplayName -> handleInsertUserDisplayName(action)
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
        copy(sendMode = SendMode.Regular(currentComposerText, action.fromSharing))
    }

    private fun handleEnterEditMode(action: MessageComposerAction.EnterEditMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            val formatted = vectorPreferences.isRichTextEditorEnabled()
            setState { copy(sendMode = SendMode.Edit(timelineEvent, timelineEvent.getTextEditableContent(formatted))) }
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

    private fun observeVoiceBroadcast() {
        room.stateService().getStateEventLive(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO, QueryStringValue.Equals(session.myUserId))
                .asFlow()
                .unwrap()
                .mapNotNull { it.asVoiceBroadcastEvent()?.content?.voiceBroadcastState }
                .setOnEach {
                    copy(voiceBroadcastState = it)
                }
    }

    private fun handleEnterQuoteMode(action: MessageComposerAction.EnterQuoteMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Quote(timelineEvent, currentComposerText)) }
        }
    }

    private fun handleEnterReplyMode(action: MessageComposerAction.EnterReplyMode) {
        room.getTimelineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Reply(timelineEvent, currentComposerText)) }
        }
    }

    private fun handleSendMessage(action: MessageComposerAction.SendMessage) {
        withState { state ->
            analyticsTracker.capture(state.toAnalyticsComposer()).also {
                setState { copy(startsThread = false) }
            }
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
                            popDraft()
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
                            popDraft()
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
                            popDraft()
                        }
                        is ParsedCommand.ChangeRoomName -> {
                            handleChangeRoomNameSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.Invite -> {
                            handleInviteSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.Invite3Pid -> {
                            handleInvite3pidSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.SetUserPowerLevel -> {
                            handleSetUserPowerLevel(parsedCommand)
                        }
                        is ParsedCommand.DevTools -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.ClearScalarToken -> {
                            // TODO
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown -> {
                            vectorPreferences.setMarkdownEnabled(parsedCommand.enable)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.BanUser -> {
                            handleBanSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.UnbanUser -> {
                            handleUnbanSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.IgnoreUser -> {
                            handleIgnoreSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.UnignoreUser -> {
                            handleUnignoreSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.RemoveUser -> {
                            handleRemoveSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.JoinRoom -> {
                            handleJoinToAnotherRoomSlashCommand(parsedCommand)
                            popDraft()
                        }
                        is ParsedCommand.PartRoom -> {
                            handlePartSlashCommand(parsedCommand)
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
                            popDraft()
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
                            popDraft()
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
                            popDraft()
                        }
                        is ParsedCommand.SendSpoiler -> {
                            val text = "[${stringProvider.getString(R.string.spoiler)}](${parsedCommand.message})"
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
                            popDraft()
                        }
                        is ParsedCommand.SendShrug -> {
                            sendPrefixedMessage("¯\\_(ツ)_/¯", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.SendLenny -> {
                            sendPrefixedMessage("( ͡° ͜ʖ ͡°)", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.SendTableFlip -> {
                            sendPrefixedMessage("(╯°□°）╯︵ ┻━┻", parsedCommand.message, state.rootThreadEventId)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.SendChatEffect -> {
                            sendChatEffect(parsedCommand)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            popDraft()
                        }
                        is ParsedCommand.ChangeTopic -> {
                            handleChangeTopicSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.ChangeDisplayName -> {
                            handleChangeDisplayNameSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.ChangeDisplayNameForRoom -> {
                            handleChangeDisplayNameForRoomSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.ChangeRoomAvatar -> {
                            handleChangeRoomAvatarSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.ChangeAvatarForRoom -> {
                            handleChangeAvatarForRoomSlashCommand(parsedCommand)
                        }
                        is ParsedCommand.ShowUser -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                            handleWhoisSlashCommand(parsedCommand)
                            popDraft()
                        }
                        is ParsedCommand.DiscardSession -> {
                            if (room.roomCryptoService().isEncrypted()) {
                                session.cryptoService().discardOutboundSession(room.roomId)
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                popDraft()
                            } else {
                                _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(parsedCommand))
                                _viewEvents.post(
                                        MessageComposerViewEvents
                                                .ShowMessage(stringProvider.getString(R.string.command_description_discard_session_not_handled))
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
                                    popDraft()
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
                                    popDraft()
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
                                    popDraft()
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
                                    popDraft()
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
                            popDraft()
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
                            room.relationService().editReply(state.sendMode.timelineEvent, it, action.text.toString(), action.formattedText)
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
                    popDraft()
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
                    popDraft()
                }
                is SendMode.Reply -> {
                    val timelineEvent = state.sendMode.timelineEvent
                    val showInThread = state.sendMode.timelineEvent.root.isThread() && state.rootThreadEventId == null
                    // If threads are disabled this will make the fallback replies visible to clients with threads enabled
                    val rootThreadEventId = if (showInThread) timelineEvent.root.getRootThreadEventId() else null
                    state.rootThreadEventId?.let {
                        room.relationService().replyInThread(
                                rootThreadEventId = it,
                                replyInThreadText = action.text.toString(),
                                autoMarkdown = action.autoMarkdown,
                                formattedText = action.formattedText,
                                eventReplied = timelineEvent
                        )
                    } ?: room.relationService().replyToMessage(
                            eventReplied = timelineEvent,
                            replyText = action.text.toString(),
                            replyFormattedText = action.formattedText,
                            autoMarkdown = action.autoMarkdown,
                            showInThread = showInThread,
                            rootThreadEventId = rootThreadEventId
                    )

                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.Voice -> {
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
                room.draftService().deleteDraft()
            }
        }
    }

    private fun loadDraftIfAny() {
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

    private fun handleUserIsTyping(action: MessageComposerAction.UserIsTyping) {
        if (vectorPreferences.sendTypingNotifs()) {
            if (action.isTyping) {
                room.typingService().userIsTyping()
            } else {
                room.typingService().userStopsTyping()
            }
        }
    }

    private fun sendChatEffect(sendChatEffect: ParsedCommand.SendChatEffect) {
        // If message is blank, convert to an emote, with default message
        if (sendChatEffect.message.isBlank()) {
            val defaultMessage = stringProvider.getString(
                    when (sendChatEffect.chatEffect) {
                        ChatEffect.CONFETTI -> R.string.default_message_emote_confetti
                        ChatEffect.SNOWFALL -> R.string.default_message_emote_snow
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

    private fun handleChangeTopicSlashCommand(changeTopic: ParsedCommand.ChangeTopic) {
        launchSlashCommandFlowSuspendable(changeTopic) {
            room.stateService().updateTopic(changeTopic.topic)
        }
    }

    private fun handleInviteSlashCommand(invite: ParsedCommand.Invite) {
        launchSlashCommandFlowSuspendable(invite) {
            room.membershipService().invite(invite.userId, invite.reason)
        }
    }

    private fun handleInvite3pidSlashCommand(invite: ParsedCommand.Invite3Pid) {
        launchSlashCommandFlowSuspendable(invite) {
            room.membershipService().invite3pid(invite.threePid)
        }
    }

    private fun handleSetUserPowerLevel(setUserPowerLevel: ParsedCommand.SetUserPowerLevel) {
        val newPowerLevelsContent = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?.setUserPowerLevel(setUserPowerLevel.userId, setUserPowerLevel.powerLevel)
                ?.toContent()
                ?: return

        launchSlashCommandFlowSuspendable(setUserPowerLevel) {
            room.stateService().sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent)
        }
    }

    private fun handleChangeDisplayNameSlashCommand(changeDisplayName: ParsedCommand.ChangeDisplayName) {
        launchSlashCommandFlowSuspendable(changeDisplayName) {
            session.profileService().setDisplayName(session.myUserId, changeDisplayName.displayName)
        }
    }

    private fun handlePartSlashCommand(command: ParsedCommand.PartRoom) {
        launchSlashCommandFlowSuspendable(command) {
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

    private fun handleRemoveSlashCommand(removeUser: ParsedCommand.RemoveUser) {
        launchSlashCommandFlowSuspendable(removeUser) {
            room.membershipService().remove(removeUser.userId, removeUser.reason)
        }
    }

    private fun handleBanSlashCommand(ban: ParsedCommand.BanUser) {
        launchSlashCommandFlowSuspendable(ban) {
            room.membershipService().ban(ban.userId, ban.reason)
        }
    }

    private fun handleUnbanSlashCommand(unban: ParsedCommand.UnbanUser) {
        launchSlashCommandFlowSuspendable(unban) {
            room.membershipService().unban(unban.userId, unban.reason)
        }
    }

    private fun handleChangeRoomNameSlashCommand(changeRoomName: ParsedCommand.ChangeRoomName) {
        launchSlashCommandFlowSuspendable(changeRoomName) {
            room.stateService().updateName(changeRoomName.name)
        }
    }

    private fun getMyRoomMemberContent(): RoomMemberContent? {
        return room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(session.myUserId))
                ?.content
                ?.toModel<RoomMemberContent>()
    }

    private fun handleChangeDisplayNameForRoomSlashCommand(changeDisplayName: ParsedCommand.ChangeDisplayNameForRoom) {
        launchSlashCommandFlowSuspendable(changeDisplayName) {
            getMyRoomMemberContent()
                    ?.copy(displayName = changeDisplayName.displayName)
                    ?.toContent()
                    ?.let {
                        room.stateService().sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleChangeRoomAvatarSlashCommand(changeAvatar: ParsedCommand.ChangeRoomAvatar) {
        launchSlashCommandFlowSuspendable(changeAvatar) {
            room.stateService().sendStateEvent(EventType.STATE_ROOM_AVATAR, stateKey = "", RoomAvatarContent(changeAvatar.url).toContent())
        }
    }

    private fun handleChangeAvatarForRoomSlashCommand(changeAvatar: ParsedCommand.ChangeAvatarForRoom) {
        launchSlashCommandFlowSuspendable(changeAvatar) {
            getMyRoomMemberContent()
                    ?.copy(avatarUrl = changeAvatar.url)
                    ?.toContent()
                    ?.let {
                        room.stateService().sendStateEvent(EventType.STATE_ROOM_MEMBER, session.myUserId, it)
                    }
        }
    }

    private fun handleIgnoreSlashCommand(ignore: ParsedCommand.IgnoreUser) {
        launchSlashCommandFlowSuspendable(ignore) {
            session.userService().ignoreUserIds(listOf(ignore.userId))
        }
    }

    private fun handleUnignoreSlashCommand(unignore: ParsedCommand.UnignoreUser) {
        _viewEvents.post(MessageComposerViewEvents.SlashCommandConfirmationRequest(unignore))
    }

    private fun handleSlashCommandConfirmed(action: MessageComposerAction.SlashCommandConfirmed) {
        when (action.parsedCommand) {
            is ParsedCommand.UnignoreUser -> handleUnignoreSlashCommandConfirmed(action.parsedCommand)
            else -> TODO("Not handled yet")
        }
    }

    private fun handleUnignoreSlashCommandConfirmed(unignore: ParsedCommand.UnignoreUser) {
        launchSlashCommandFlowSuspendable(unignore) {
            session.userService().unIgnoreUserIds(listOf(unignore.userId))
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
            room.relationService().replyInThread(it, sequence)
        } ?: room.sendService().sendTextMessage(sequence)
    }

    /**
     * Convert a send mode to a draft and save the draft.
     */
    private fun handleSaveTextDraft(draft: String) = withState {
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
        audioMessageHelper.clearTracker()
        audioMessageHelper.stopAllVoiceActions(deleteRecord)
    }

    private fun handleInitializeVoiceRecorder(attachmentData: ContentAttachmentData) {
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

    private fun handleEntersBackground(composerText: String) {
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
            handleSaveTextDraft(draft = composerText)
        }
    }

    private fun handleInsertUserDisplayName(action: MessageComposerAction.InsertUserDisplayName) {
        _viewEvents.post(MessageComposerViewEvents.InsertUserDisplayName(action.userId))
    }

    private fun launchSlashCommandFlowSuspendable(parsedCommand: ParsedCommand, block: suspend () -> Unit) {
        _viewEvents.post(MessageComposerViewEvents.SlashCommandLoading)
        viewModelScope.launch {
            val event = try {
                block()
                popDraft()
                MessageComposerViewEvents.SlashCommandResultOk(parsedCommand)
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
