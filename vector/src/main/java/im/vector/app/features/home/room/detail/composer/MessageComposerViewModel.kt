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
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
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
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getRelationContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import timber.log.Timber

class MessageComposerViewModel @AssistedInject constructor(
        @Assisted initialState: MessageComposerViewState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val rainbowGenerator: RainbowGenerator,
        private val voiceMessageHelper: VoiceMessageHelper,
        private val voicePlayerHelper: VoicePlayerHelper
) : VectorViewModel<MessageComposerViewState, MessageComposerAction, MessageComposerViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    // Keep it out of state to avoid invalidate being called
    private var currentComposerText: CharSequence = ""

    init {
        loadDraftIfAny()
        observePowerLevel()
        subscribeToStateInternal()
    }

    override fun handle(action: MessageComposerAction) {
        Timber.v("Handle action: $action")
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
            is MessageComposerAction.EndRecordingVoiceMessage       -> handleEndRecordingVoiceMessage(action.isCancelled)
            is MessageComposerAction.PlayOrPauseVoicePlayback       -> handlePlayOrPauseVoicePlayback(action)
            MessageComposerAction.PauseRecordingVoiceMessage        -> handlePauseRecordingVoiceMessage()
            MessageComposerAction.PlayOrPauseRecordingPlayback      -> handlePlayOrPauseRecordingPlayback()
            is MessageComposerAction.EndAllVoiceActions             -> handleEndAllVoiceActions(action.deleteRecord)
            is MessageComposerAction.InitializeVoiceRecorder        -> handleInitializeVoiceRecorder(action.attachmentData)
            is MessageComposerAction.OnEntersBackground             -> handleEntersBackground(action.composerText)
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
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Edit(timelineEvent, timelineEvent.getTextEditableContent())) }
        }
    }

    private fun observePowerLevel() {
        PowerLevelsFlowFactory(room).createFlow()
                .setOnEach {
                    val canSendMessage = PowerLevelsHelper(it).isUserAllowedToSend(session.myUserId, false, EventType.MESSAGE)
                    copy(canSendMessage = canSendMessage)
                }
    }

    private fun handleEnterQuoteMode(action: MessageComposerAction.EnterQuoteMode) {
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Quote(timelineEvent, action.text)) }
        }
    }

    private fun handleEnterReplyMode(action: MessageComposerAction.EnterReplyMode) {
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.Reply(timelineEvent, action.text)) }
        }
    }

    private fun handleSendMessage(action: MessageComposerAction.SendMessage) {
        withState { state ->
            when (state.sendMode) {
                is SendMode.Regular -> {
                    when (val slashCommandResult = CommandParser.parseSplashCommand(action.text)) {
                        is ParsedCommand.ErrorNotACommand         -> {
                            // Send the text message to the room
                            room.sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ErrorSyntax              -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandError(slashCommandResult.command))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand   -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown("/"))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandUnknown(slashCommandResult.slashCommand))
                        }
                        is ParsedCommand.SendPlainText            -> {
                            // Send the text message to the room, without markdown
                            room.sendTextMessage(slashCommandResult.message, autoMarkdown = false)
                            _viewEvents.post(MessageComposerViewEvents.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ChangeRoomName           -> {
                            handleChangeRoomNameSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.Invite                   -> {
                            handleInviteSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.Invite3Pid               -> {
                            handleInvite3pidSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.SetUserPowerLevel        -> {
                            handleSetUserPowerLevel(slashCommandResult)
                        }
                        is ParsedCommand.ClearScalarToken         -> {
                            // TODO
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown              -> {
                            vectorPreferences.setMarkdownEnabled(slashCommandResult.enable)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk(
                                    if (slashCommandResult.enable) R.string.markdown_has_been_enabled else R.string.markdown_has_been_disabled))
                            popDraft()
                        }
                        is ParsedCommand.BanUser                  -> {
                            handleBanSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.UnbanUser                -> {
                            handleUnbanSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.IgnoreUser               -> {
                            handleIgnoreSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.UnignoreUser             -> {
                            handleUnignoreSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.KickUser                 -> {
                            handleKickSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.JoinRoom                 -> {
                            handleJoinToAnotherRoomSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.PartRoom                 -> {
                            handlePartSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.SendEmote                -> {
                            room.sendTextMessage(slashCommandResult.message, msgType = MessageType.MSGTYPE_EMOTE, autoMarkdown = action.autoMarkdown)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbow              -> {
                            slashCommandResult.message.toString().let {
                                room.sendFormattedTextMessage(it, rainbowGenerator.generate(it))
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbowEmote         -> {
                            slashCommandResult.message.toString().let {
                                room.sendFormattedTextMessage(it, rainbowGenerator.generate(it), MessageType.MSGTYPE_EMOTE)
                            }
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendSpoiler              -> {
                            room.sendFormattedTextMessage(
                                    "[${stringProvider.getString(R.string.spoiler)}](${slashCommandResult.message})",
                                    "<span data-mx-spoiler>${slashCommandResult.message}</span>"
                            )
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendShrug                -> {
                            sendPrefixedMessage("¯\\_(ツ)_/¯", slashCommandResult.message)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendLenny                -> {
                            sendPrefixedMessage("( ͡° ͜ʖ ͡°)", slashCommandResult.message)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendChatEffect           -> {
                            sendChatEffect(slashCommandResult)
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.ChangeTopic              -> {
                            handleChangeTopicSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeDisplayName        -> {
                            handleChangeDisplayNameSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeDisplayNameForRoom -> {
                            handleChangeDisplayNameForRoomSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeRoomAvatar         -> {
                            handleChangeRoomAvatarSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ChangeAvatarForRoom      -> {
                            handleChangeAvatarForRoomSlashCommand(slashCommandResult)
                        }
                        is ParsedCommand.ShowUser                 -> {
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            handleWhoisSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.DiscardSession           -> {
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
                        is ParsedCommand.CreateSpace              -> {
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
                        is ParsedCommand.AddToSpace               -> {
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
                        is ParsedCommand.JoinSpace                -> {
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
                        is ParsedCommand.LeaveRoom                -> {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.getRoom(slashCommandResult.roomId)?.leave(null)
                                    popDraft()
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(MessageComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.UpgradeRoom              -> {
                            _viewEvents.post(
                                    MessageComposerViewEvents.ShowRoomUpgradeDialog(
                                            slashCommandResult.newVersion,
                                            room.roomSummary()?.isPublic ?: false
                                    )
                            )
                            _viewEvents.post(MessageComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                    }.exhaustive
                }
                is SendMode.Edit    -> {
                    // is original event a reply?
                    val inReplyTo = state.sendMode.timelineEvent.getRelationContent()?.inReplyTo?.eventId
                    if (inReplyTo != null) {
                        // TODO check if same content?
                        room.getTimeLineEvent(inReplyTo)?.let {
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
                    val messageContent = state.sendMode.timelineEvent.getLastMessageContent()
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
                    _viewEvents.post(MessageComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.Reply   -> {
                    state.sendMode.timelineEvent.let {
                        room.replyToMessage(it, action.text.toString(), action.autoMarkdown)
                        _viewEvents.post(MessageComposerViewEvents.MessageSent)
                        popDraft()
                    }
                }
                is SendMode.Voice   -> {
                    // do nothing
                }
            }.exhaustive
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
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Quote(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Reply   -> {
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.Reply(timelineEvent, currentDraft.content)
                            }
                        }
                        is UserDraft.Edit    -> {
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
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
            room.sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, null, newPowerLevelsContent)
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
                    ?.leave(reason = null)
        }
    }

    private fun handleKickSlashCommand(kick: ParsedCommand.KickUser) {
        launchSlashCommandFlowSuspendable {
            room.kick(kick.userId, kick.reason)
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
            room.sendStateEvent(EventType.STATE_ROOM_AVATAR, null, RoomAvatarContent(changeAvatar.url).toContent())
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

    private fun sendPrefixedMessage(prefix: String, message: CharSequence) {
        val sequence = buildString {
            append(prefix)
            if (message.isNotEmpty()) {
                append(" ")
                append(message)
            }
        }
        room.sendTextMessage(sequence)
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
            voiceMessageHelper.startRecording(room.roomId)
        } catch (failure: Throwable) {
            _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
        }
    }

    private fun handleEndRecordingVoiceMessage(isCancelled: Boolean) {
        voiceMessageHelper.stopPlayback()
        if (isCancelled) {
            voiceMessageHelper.deleteRecording()
        } else {
            voiceMessageHelper.stopRecording(convertForSending = true)?.let { audioType ->
                if (audioType.duration > 1000) {
                    room.sendMedia(audioType.toContentAttachmentData(isVoiceMessage = true), false, emptySet())
                } else {
                    voiceMessageHelper.deleteRecording()
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
                voiceMessageHelper.startOrPausePlayback(action.eventId, convertedFile)
            } catch (failure: Throwable) {
                _viewEvents.post(MessageComposerViewEvents.VoicePlaybackOrRecordingFailure(failure))
            }
        }
    }

    private fun handlePlayOrPauseRecordingPlayback() {
        voiceMessageHelper.startOrPauseRecordingPlayback()
    }

    private fun handleEndAllVoiceActions(deleteRecord: Boolean) {
        voiceMessageHelper.clearTracker()
        voiceMessageHelper.stopAllVoiceActions(deleteRecord)
    }

    private fun handleInitializeVoiceRecorder(attachmentData: ContentAttachmentData) {
        voiceMessageHelper.initializeRecorder(attachmentData)
        setState { copy(voiceRecordingUiState = VoiceMessageRecorderView.RecordingUiState.Draft) }
    }

    private fun handlePauseRecordingVoiceMessage() {
        voiceMessageHelper.pauseRecording()
    }

    private fun handleEntersBackground(composerText: String) {
        val isVoiceRecording = com.airbnb.mvrx.withState(this) { it.isVoiceRecording }
        if (isVoiceRecording) {
            voiceMessageHelper.clearTracker()
            viewModelScope.launch {
                voiceMessageHelper.stopAllVoiceActions(deleteRecord = false)?.toContentAttachmentData()?.let { voiceDraft ->
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
