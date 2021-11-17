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
import im.vector.app.features.command.CommandParser
import im.vector.app.features.command.ParsedCommand
import im.vector.app.features.home.room.detail.ChatEffect
import im.vector.app.features.home.room.detail.composer.rainbow.RainbowGenerator
import im.vector.app.features.home.room.detail.toMessageType
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
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

class TextComposerViewModel @AssistedInject constructor(
        @Assisted initialState: TextComposerViewState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val rainbowGenerator: RainbowGenerator
) : VectorViewModel<TextComposerViewState, TextComposerAction, TextComposerViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    // Keep it out of state to avoid invalidate being called
    private var currentComposerText: CharSequence = ""

    init {
        loadDraftIfAny()
        observePowerLevel()
        subscribeToStateInternal()
    }

    override fun handle(action: TextComposerAction) {
        Timber.v("Handle action: $action")
        when (action) {
            is TextComposerAction.EnterEditMode                -> handleEnterEditMode(action)
            is TextComposerAction.EnterQuoteMode               -> handleEnterQuoteMode(action)
            is TextComposerAction.EnterRegularMode             -> handleEnterRegularMode(action)
            is TextComposerAction.EnterReplyMode               -> handleEnterReplyMode(action)
            is TextComposerAction.SaveDraft                    -> handleSaveDraft(action)
            is TextComposerAction.SendMessage                  -> handleSendMessage(action)
            is TextComposerAction.UserIsTyping                 -> handleUserIsTyping(action)
            is TextComposerAction.OnTextChanged                -> handleOnTextChanged(action)
            is TextComposerAction.OnVoiceRecordingStateChanged -> handleOnVoiceRecordingStateChanged(action)
        }
    }

    private fun handleOnVoiceRecordingStateChanged(action: TextComposerAction.OnVoiceRecordingStateChanged) = setState {
        copy(isVoiceRecording = action.isRecording)
    }

    private fun handleOnTextChanged(action: TextComposerAction.OnTextChanged) {
        setState {
            // Makes sure currentComposerText is upToDate when accessing further setState
            currentComposerText = action.text
            this
        }
        updateIsSendButtonVisibility(true)
    }

    private fun subscribeToStateInternal() {
        onEach(TextComposerViewState::sendMode, TextComposerViewState::canSendMessage, TextComposerViewState::isVoiceRecording) { _, _, _ ->
            updateIsSendButtonVisibility(false)
        }
    }

    private fun updateIsSendButtonVisibility(triggerAnimation: Boolean) = setState {
        val isSendButtonVisible = isComposerVisible && (sendMode !is SendMode.REGULAR || currentComposerText.isNotBlank())
        if (this.isSendButtonVisible != isSendButtonVisible && triggerAnimation) {
            _viewEvents.post(TextComposerViewEvents.AnimateSendButtonVisibility(isSendButtonVisible))
        }
        copy(isSendButtonVisible = isSendButtonVisible)
    }

    private fun handleEnterRegularMode(action: TextComposerAction.EnterRegularMode) = setState {
        copy(sendMode = SendMode.REGULAR(action.text, action.fromSharing))
    }

    private fun handleEnterEditMode(action: TextComposerAction.EnterEditMode) {
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.EDIT(timelineEvent, timelineEvent.getTextEditableContent())) }
        }
    }

    private fun observePowerLevel() {
        PowerLevelsFlowFactory(room).createFlow()
                .setOnEach {
                    val canSendMessage = PowerLevelsHelper(it).isUserAllowedToSend(session.myUserId, false, EventType.MESSAGE)
                    copy(canSendMessage = canSendMessage)
                }
    }

    private fun handleEnterQuoteMode(action: TextComposerAction.EnterQuoteMode) {
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.QUOTE(timelineEvent, action.text)) }
        }
    }

    private fun handleEnterReplyMode(action: TextComposerAction.EnterReplyMode) {
        room.getTimeLineEvent(action.eventId)?.let { timelineEvent ->
            setState { copy(sendMode = SendMode.REPLY(timelineEvent, action.text)) }
        }
    }

    private fun handleSendMessage(action: TextComposerAction.SendMessage) {
        withState { state ->
            when (state.sendMode) {
                is SendMode.REGULAR -> {
                    when (val slashCommandResult = CommandParser.parseSplashCommand(action.text)) {
                        is ParsedCommand.ErrorNotACommand         -> {
                            // Send the text message to the room
                            room.sendTextMessage(action.text, autoMarkdown = action.autoMarkdown)
                            _viewEvents.post(TextComposerViewEvents.MessageSent)
                            popDraft()
                        }
                        is ParsedCommand.ErrorSyntax              -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandError(slashCommandResult.command))
                        }
                        is ParsedCommand.ErrorEmptySlashCommand   -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandUnknown("/"))
                        }
                        is ParsedCommand.ErrorUnknownSlashCommand -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandUnknown(slashCommandResult.slashCommand))
                        }
                        is ParsedCommand.SendPlainText            -> {
                            // Send the text message to the room, without markdown
                            room.sendTextMessage(slashCommandResult.message, autoMarkdown = false)
                            _viewEvents.post(TextComposerViewEvents.MessageSent)
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
                            _viewEvents.post(TextComposerViewEvents.SlashCommandNotImplemented)
                        }
                        is ParsedCommand.SetMarkdown              -> {
                            vectorPreferences.setMarkdownEnabled(slashCommandResult.enable)
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk(
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
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbow              -> {
                            slashCommandResult.message.toString().let {
                                room.sendFormattedTextMessage(it, rainbowGenerator.generate(it))
                            }
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendRainbowEmote         -> {
                            slashCommandResult.message.toString().let {
                                room.sendFormattedTextMessage(it, rainbowGenerator.generate(it), MessageType.MSGTYPE_EMOTE)
                            }
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendSpoiler              -> {
                            room.sendFormattedTextMessage(
                                    "[${stringProvider.getString(R.string.spoiler)}](${slashCommandResult.message})",
                                    "<span data-mx-spoiler>${slashCommandResult.message}</span>"
                            )
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendShrug                -> {
                            sendPrefixedMessage("¯\\_(ツ)_/¯", slashCommandResult.message)
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendLenny                -> {
                            sendPrefixedMessage("( ͡° ͜ʖ ͡°)", slashCommandResult.message)
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                        is ParsedCommand.SendChatEffect           -> {
                            sendChatEffect(slashCommandResult)
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
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
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            handleWhoisSlashCommand(slashCommandResult)
                            popDraft()
                        }
                        is ParsedCommand.DiscardSession           -> {
                            if (room.isEncrypted()) {
                                session.cryptoService().discardOutboundSession(room.roomId)
                                _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                popDraft()
                            } else {
                                _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                _viewEvents.post(
                                        TextComposerViewEvents
                                                .ShowMessage(stringProvider.getString(R.string.command_description_discard_session_not_handled))
                                )
                            }
                        }
                        is ParsedCommand.CreateSpace              -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandLoading)
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
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.AddToSpace               -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandLoading)
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
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.JoinSpace                -> {
                            _viewEvents.post(TextComposerViewEvents.SlashCommandLoading)
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.spaceService().joinSpace(slashCommandResult.spaceIdOrAlias)
                                    popDraft()
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.LeaveRoom                -> {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    session.getRoom(slashCommandResult.roomId)?.leave(null)
                                    popDraft()
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                                } catch (failure: Throwable) {
                                    _viewEvents.post(TextComposerViewEvents.SlashCommandResultError(failure))
                                }
                            }
                            Unit
                        }
                        is ParsedCommand.UpgradeRoom              -> {
                            _viewEvents.post(
                                    TextComposerViewEvents.ShowRoomUpgradeDialog(
                                            slashCommandResult.newVersion,
                                            room.roomSummary()?.isPublic ?: false
                                    )
                            )
                            _viewEvents.post(TextComposerViewEvents.SlashCommandResultOk())
                            popDraft()
                        }
                    }.exhaustive
                }
                is SendMode.EDIT    -> {
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
                    _viewEvents.post(TextComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.QUOTE   -> {
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
                    _viewEvents.post(TextComposerViewEvents.MessageSent)
                    popDraft()
                }
                is SendMode.REPLY   -> {
                    state.sendMode.timelineEvent.let {
                        room.replyToMessage(it, action.text.toString(), action.autoMarkdown)
                        _viewEvents.post(TextComposerViewEvents.MessageSent)
                        popDraft()
                    }
                }
            }.exhaustive
        }
    }

    private fun popDraft() = withState {
        if (it.sendMode is SendMode.REGULAR && it.sendMode.fromSharing) {
            // If we were sharing, we want to get back our last value from draft
            loadDraftIfAny()
        } else {
            // Otherwise we clear the composer and remove the draft from db
            setState { copy(sendMode = SendMode.REGULAR("", false)) }
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
                        is UserDraft.REGULAR -> SendMode.REGULAR(currentDraft.text, false)
                        is UserDraft.QUOTE   -> {
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.QUOTE(timelineEvent, currentDraft.text)
                            }
                        }
                        is UserDraft.REPLY   -> {
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.REPLY(timelineEvent, currentDraft.text)
                            }
                        }
                        is UserDraft.EDIT    -> {
                            room.getTimeLineEvent(currentDraft.linkedEventId)?.let { timelineEvent ->
                                SendMode.EDIT(timelineEvent, currentDraft.text)
                            }
                        }
                        else                 -> null
                    } ?: SendMode.REGULAR("", fromSharing = false)
            )
        }
    }

    private fun handleUserIsTyping(action: TextComposerAction.UserIsTyping) {
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
                _viewEvents.post(TextComposerViewEvents.SlashCommandResultError(failure))
                return@launch
            }
            session.getRoomSummary(command.roomAlias)
                    ?.roomId
                    ?.let {
                        _viewEvents.post(TextComposerViewEvents.JoinRoomCommandSuccess(it))
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
        _viewEvents.post(TextComposerViewEvents.OpenRoomMemberProfile(whois.userId))
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
    private fun handleSaveDraft(action: TextComposerAction.SaveDraft) = withState {
        session.coroutineScope.launch {
            when {
                it.sendMode is SendMode.REGULAR && !it.sendMode.fromSharing -> {
                    setState { copy(sendMode = it.sendMode.copy(action.draft)) }
                    room.saveDraft(UserDraft.REGULAR(action.draft))
                }
                it.sendMode is SendMode.REPLY                               -> {
                    setState { copy(sendMode = it.sendMode.copy(text = action.draft)) }
                    room.saveDraft(UserDraft.REPLY(it.sendMode.timelineEvent.root.eventId!!, action.draft))
                }
                it.sendMode is SendMode.QUOTE                               -> {
                    setState { copy(sendMode = it.sendMode.copy(text = action.draft)) }
                    room.saveDraft(UserDraft.QUOTE(it.sendMode.timelineEvent.root.eventId!!, action.draft))
                }
                it.sendMode is SendMode.EDIT                                -> {
                    setState { copy(sendMode = it.sendMode.copy(text = action.draft)) }
                    room.saveDraft(UserDraft.EDIT(it.sendMode.timelineEvent.root.eventId!!, action.draft))
                }
            }
        }
    }

    private fun launchSlashCommandFlowSuspendable(block: suspend () -> Unit) {
        _viewEvents.post(TextComposerViewEvents.SlashCommandLoading)
        viewModelScope.launch {
            val event = try {
                block()
                popDraft()
                TextComposerViewEvents.SlashCommandResultOk()
            } catch (failure: Throwable) {
                TextComposerViewEvents.SlashCommandResultError(failure)
            }
            _viewEvents.post(event)
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<TextComposerViewModel, TextComposerViewState> {
        override fun create(initialState: TextComposerViewState): TextComposerViewModel
    }

    companion object : MavericksViewModelFactory<TextComposerViewModel, TextComposerViewState> by hiltMavericksViewModelFactory()
}
