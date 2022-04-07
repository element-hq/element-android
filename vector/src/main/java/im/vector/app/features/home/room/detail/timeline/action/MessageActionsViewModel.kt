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
package im.vector.app.features.home.room.detail.timeline.action

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.canReact
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.PillsPostProcessor
import im.vector.app.features.html.VectorHtmlCompressor
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isAttachmentMessage
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.hasBeenEdited
import org.matrix.android.sdk.api.session.room.timeline.isPoll
import org.matrix.android.sdk.api.session.room.timeline.isRootThread
import org.matrix.android.sdk.api.session.room.timeline.isSticker
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

/**
 * Information related to an event and used to display preview in contextual bottom sheet.
 */
class MessageActionsViewModel @AssistedInject constructor(
        @Assisted private val initialState: MessageActionState,
        private val eventHtmlRenderer: Lazy<EventHtmlRenderer>,
        private val htmlCompressor: VectorHtmlCompressor,
        private val session: Session,
        private val noticeEventFormatter: NoticeEventFormatter,
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider,
        private val pillsPostProcessorFactory: PillsPostProcessor.Factory,
        private val vectorPreferences: VectorPreferences
) : VectorViewModel<MessageActionState, MessageActionsAction, EmptyViewEvents>(initialState) {

    private val informationData = initialState.informationData
    private val room = session.getRoom(initialState.roomId)
    private val pillsPostProcessor by lazy {
        pillsPostProcessorFactory.create(initialState.roomId)
    }

    private val eventIdFlow = MutableStateFlow(initialState.eventId)

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<MessageActionsViewModel, MessageActionState> {
        override fun create(initialState: MessageActionState): MessageActionsViewModel
    }

    companion object : MavericksViewModelFactory<MessageActionsViewModel, MessageActionState> by hiltMavericksViewModelFactory()

    init {
        observeEvent()
        observeReactions()
        observePowerLevel()
        observeTimelineEventState()
    }

    override fun handle(action: MessageActionsAction) {
        when (action) {
            MessageActionsAction.ToggleReportMenu -> toggleReportMenu()
        }
    }

    private fun toggleReportMenu() = withState {
        setState {
            copy(
                    expendedReportContentMenu = it.expendedReportContentMenu.not()
            )
        }
    }

    private fun observePowerLevel() {
        if (room == null) {
            return
        }
        PowerLevelsFlowFactory(room).createFlow()
                .onEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val canReact = powerLevelsHelper.isUserAllowedToSend(session.myUserId, false, EventType.REACTION)
                    val canRedact = powerLevelsHelper.isUserAbleToRedact(session.myUserId)
                    val canSendMessage = powerLevelsHelper.isUserAllowedToSend(session.myUserId, false, EventType.MESSAGE)
                    val permissions = ActionPermissions(canSendMessage = canSendMessage, canRedact = canRedact, canReact = canReact)
                    setState {
                        copy(actionPermissions = permissions)
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeEvent() {
        if (room == null) return
        room.flow()
                .liveTimelineEvent(initialState.eventId)
                .unwrap()
                .execute {
                    copy(timelineEvent = it)
                }
    }

    private fun observeReactions() {
        if (room == null) return
        eventIdFlow
                .flatMapLatest { eventId ->
                    room.flow()
                            .liveAnnotationSummary(eventId)
                            .map { annotations ->
                                EmojiDataSource.quickEmojis.map { emoji ->
                                    ToggleState(emoji, annotations.getOrNull()?.reactionsSummary?.firstOrNull { it.key == emoji }?.addedByMe ?: false)
                                }
                            }
                }
                .execute {
                    copy(quickStates = it)
                }
    }

    private fun observeTimelineEventState() {
        onEach(MessageActionState::timelineEvent, MessageActionState::actionPermissions) { timelineEvent, permissions ->
            val nonNullTimelineEvent = timelineEvent() ?: return@onEach
            eventIdFlow.tryEmit(nonNullTimelineEvent.eventId)
            setState {
                copy(
                        eventId = nonNullTimelineEvent.eventId,
                        messageBody = computeMessageBody(nonNullTimelineEvent),
                        actions = actionsForEvent(nonNullTimelineEvent, permissions)
                )
            }
        }
    }

    private fun computeMessageBody(timelineEvent: TimelineEvent): CharSequence {
        return try {
            if (timelineEvent.root.isRedacted()) {
                noticeEventFormatter.formatRedactedEvent(timelineEvent.root)
            } else {
                when (timelineEvent.root.getClearType()) {
                    EventType.MESSAGE,
                    EventType.STICKER       -> {
                        val messageContent: MessageContent? = timelineEvent.getLastMessageContent()
                        if (messageContent is MessageTextContent && messageContent.format == MessageFormat.FORMAT_MATRIX_HTML) {
                            val html = messageContent.formattedBody
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { htmlCompressor.compress(it) }
                                    ?: messageContent.body

                            eventHtmlRenderer.get().render(html, pillsPostProcessor)
                        } else if (messageContent is MessageVerificationRequestContent) {
                            stringProvider.getString(R.string.verification_request)
                        } else {
                            messageContent?.body
                        }
                    }
                    EventType.STATE_ROOM_NAME,
                    EventType.STATE_ROOM_TOPIC,
                    EventType.STATE_ROOM_AVATAR,
                    EventType.STATE_ROOM_MEMBER,
                    EventType.STATE_ROOM_ALIASES,
                    EventType.STATE_ROOM_CANONICAL_ALIAS,
                    EventType.STATE_ROOM_HISTORY_VISIBILITY,
                    EventType.STATE_ROOM_SERVER_ACL,
                    EventType.CALL_INVITE,
                    EventType.CALL_CANDIDATES,
                    EventType.CALL_HANGUP,
                    EventType.CALL_ANSWER   -> {
                        noticeEventFormatter.format(timelineEvent, room?.roomSummary()?.isDirect.orFalse())
                    }
                    in EventType.POLL_START -> {
                        timelineEvent.root.getClearContent().toModel<MessagePollContent>(catchError = true)
                                ?.getBestPollCreationInfo()?.question?.getBestQuestion() ?: ""
                    }
                    else                    -> null
                }
            }
        } catch (failure: Throwable) {
            errorFormatter.toHumanReadable(failure)
        } ?: ""
    }

    private fun getRedactionReason(timelineEvent: TimelineEvent): String {
        return (timelineEvent
                .root
                .unsignedData
                ?.redactedEvent
                ?.content
                ?.get("reason") as? String)
                ?.takeIf { it.isNotBlank() }
                .let { reason ->
                    if (reason == null) {
                        if (timelineEvent.root.isRedactedBySameUser()) {
                            stringProvider.getString(R.string.event_redacted_by_user_reason)
                        } else {
                            stringProvider.getString(R.string.event_redacted_by_admin_reason)
                        }
                    } else {
                        if (timelineEvent.root.isRedactedBySameUser()) {
                            stringProvider.getString(R.string.event_redacted_by_user_reason_with_reason, reason)
                        } else {
                            stringProvider.getString(R.string.event_redacted_by_admin_reason_with_reason, reason)
                        }
                    }
                }
    }

    private fun actionsForEvent(timelineEvent: TimelineEvent, actionPermissions: ActionPermissions): List<EventSharedAction> {
        val messageContent = timelineEvent.getLastMessageContent()
        val msgType = messageContent?.msgType

        return arrayListOf<EventSharedAction>().apply {
            when {
                timelineEvent.root.sendState.hasFailed()         -> {
                    addActionsForFailedState(timelineEvent, actionPermissions, messageContent, msgType)
                }
                timelineEvent.root.sendState.isSending()         -> {
                    addActionsForSendingState(timelineEvent)
                }
                timelineEvent.root.sendState == SendState.SYNCED -> {
                    addActionsForSyncedState(timelineEvent, actionPermissions, messageContent, msgType)
                }
                timelineEvent.root.sendState == SendState.SENT   -> {
                    addActionsForSentNotSyncedState(timelineEvent)
                }
            }
        }
    }

    private fun ArrayList<EventSharedAction>.addViewSourceItems(timelineEvent: TimelineEvent) {
        add(EventSharedAction.ViewSource(timelineEvent.root.toContentStringWithIndent()))
        if (timelineEvent.isEncrypted() && timelineEvent.root.mxDecryptionResult != null) {
            val decryptedContent = timelineEvent.root.toClearContentStringWithIndent()
                    ?: stringProvider.getString(R.string.encryption_information_decryption_error)
            add(EventSharedAction.ViewDecryptedSource(decryptedContent))
        }
    }

    private fun ArrayList<EventSharedAction>.addActionsForFailedState(timelineEvent: TimelineEvent,
                                                                      actionPermissions: ActionPermissions,
                                                                      messageContent: MessageContent?,
                                                                      msgType: String?) {
        val eventId = timelineEvent.eventId
        if (canRetry(timelineEvent, actionPermissions)) {
            add(EventSharedAction.Resend(eventId))
        }
        add(EventSharedAction.Remove(eventId))
        if (canEdit(timelineEvent, session.myUserId, actionPermissions)) {
            add(EventSharedAction.Edit(eventId, timelineEvent.root.getClearType()))
        }
        if (canCopy(msgType)) {
            // TODO copy images? html? see ClipBoard
            add(EventSharedAction.Copy(messageContent!!.body))
        }
        if (vectorPreferences.developerMode()) {
            addViewSourceItems(timelineEvent)
        }
    }

    private fun ArrayList<EventSharedAction>.addActionsForSendingState(timelineEvent: TimelineEvent) {
        // TODO is uploading attachment?
        if (canCancel(timelineEvent)) {
            add(EventSharedAction.Cancel(timelineEvent.eventId, false))
        }
    }

    private fun ArrayList<EventSharedAction>.addActionsForSentNotSyncedState(timelineEvent: TimelineEvent) {
        // If sent but not synced (synapse stuck at bottom bug)
        // Still offer action to cancel (will only remove local echo)
        timelineEvent.root.eventId?.let {
            add(EventSharedAction.Cancel(it, true))
        }

        // TODO Can be redacted

        // TODO sent by me or sufficient power level
    }

    private fun ArrayList<EventSharedAction>.addActionsForSyncedState(timelineEvent: TimelineEvent,
                                                                      actionPermissions: ActionPermissions,
                                                                      messageContent: MessageContent?,
                                                                      msgType: String?) {
        val eventId = timelineEvent.eventId
        if (!timelineEvent.root.isRedacted()) {
            if (canReply(timelineEvent, messageContent, actionPermissions)) {
                add(EventSharedAction.Reply(eventId))
            }

            if (canReplyInThread(timelineEvent, messageContent, actionPermissions)) {
                add(EventSharedAction.ReplyInThread(eventId, !timelineEvent.isRootThread()))
            }

            if (canViewInRoom(timelineEvent, messageContent, actionPermissions)) {
                add(EventSharedAction.ViewInRoom)
            }

            if (canEndPoll(timelineEvent, actionPermissions)) {
                add(EventSharedAction.EndPoll(timelineEvent.eventId))
            }

            if (canEdit(timelineEvent, session.myUserId, actionPermissions)) {
                add(EventSharedAction.Edit(eventId, timelineEvent.root.getClearType()))
            }

            if (canCopy(msgType)) {
                // TODO copy images? html? see ClipBoard
                add(EventSharedAction.Copy(messageContent!!.body))
            }

            if (timelineEvent.canReact() && actionPermissions.canReact) {
                add(EventSharedAction.AddReaction(eventId))
            }

            if (canViewReactions(timelineEvent)) {
                add(EventSharedAction.ViewReactions(informationData))
            }

            if (canQuote(timelineEvent, messageContent, actionPermissions)) {
                add(EventSharedAction.Quote(eventId))
            }

            if (timelineEvent.hasBeenEdited()) {
                add(EventSharedAction.ViewEditHistory(informationData))
            }

            if (canSave(msgType) && messageContent is MessageWithAttachmentContent) {
                add(EventSharedAction.Save(timelineEvent.eventId, messageContent))
            }

            if (canShare(msgType)) {
                add(EventSharedAction.Share(timelineEvent.eventId, messageContent!!))
            }

            if (canRedact(timelineEvent, actionPermissions)) {
                if (timelineEvent.root.getClearType() in EventType.POLL_START) {
                    add(EventSharedAction.Redact(
                            eventId,
                            askForReason = informationData.senderId != session.myUserId,
                            dialogTitleRes = R.string.delete_poll_dialog_title,
                            dialogDescriptionRes = R.string.delete_poll_dialog_content
                    ))
                } else {
                    add(EventSharedAction.Redact(
                            eventId,
                            askForReason = informationData.senderId != session.myUserId,
                            dialogTitleRes = R.string.delete_event_dialog_title,
                            dialogDescriptionRes = R.string.delete_event_dialog_content
                    ))
                }
            }
        }

        if (vectorPreferences.developerMode()) {
            if (timelineEvent.isEncrypted() && timelineEvent.root.mCryptoError != null) {
                val keysBackupService = session.cryptoService().keysBackupService()
                if (keysBackupService.state == KeysBackupState.NotTrusted ||
                        (keysBackupService.state == KeysBackupState.ReadyToBackUp &&
                                keysBackupService.canRestoreKeys())
                ) {
                    add(EventSharedAction.UseKeyBackup)
                }
                if (session.cryptoService().getCryptoDeviceInfo(session.myUserId).size > 1 ||
                        timelineEvent.senderInfo.userId != session.myUserId) {
                    add(EventSharedAction.ReRequestKey(timelineEvent.eventId))
                }
            }
            addViewSourceItems(timelineEvent)
        }
        add(EventSharedAction.CopyPermalink(eventId))
        if (session.myUserId != timelineEvent.root.senderId) {
            // not sent by me
            if (timelineEvent.root.getClearType() == EventType.MESSAGE) {
                add(EventSharedAction.ReportContent(eventId, timelineEvent.root.senderId))
            }

            add(EventSharedAction.Separator)
            add(EventSharedAction.IgnoreUser(timelineEvent.root.senderId))
        }
    }

    private fun canCancel(@Suppress("UNUSED_PARAMETER") event: TimelineEvent): Boolean {
        return true
    }

    private fun canReply(event: TimelineEvent, messageContent: MessageContent?, actionPermissions: ActionPermissions): Boolean {
        // Only EventType.MESSAGE and EventType.POLL_START event types are supported for the moment
        if (event.root.getClearType() !in EventType.POLL_START + EventType.MESSAGE) return false
        if (!actionPermissions.canSendMessage) return false
        return when (messageContent?.msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_FILE,
            MessageType.MSGTYPE_POLL_START,
            MessageType.MSGTYPE_LOCATION -> true
            else                         -> false
        }
    }

    /**
     * Determine whether or not the Reply In Thread bottom sheet action will be visible
     * to the user
     */
    private fun canReplyInThread(event: TimelineEvent,
                                 messageContent: MessageContent?,
                                 actionPermissions: ActionPermissions): Boolean {
        // We let reply in thread visible even if threads are not enabled, with an enhanced flow to attract users
//        if (!vectorPreferences.areThreadMessagesEnabled()) return false
        if (initialState.isFromThreadTimeline) return false
        if (event.root.isThread()) return false
        if (event.root.getClearType() != EventType.MESSAGE &&
                !event.isSticker() && !event.isPoll()) return false
        if (!actionPermissions.canSendMessage) return false
        return when (messageContent?.msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_FILE,
            MessageType.MSGTYPE_POLL_START,
            MessageType.MSGTYPE_STICKER_LOCAL -> true
            else                              -> false
        }
    }

    /**
     * Determine whether or not the view in room action will be available for the current event
     */
    private fun canViewInRoom(event: TimelineEvent,
                              messageContent: MessageContent?,
                              actionPermissions: ActionPermissions): Boolean {
        if (!vectorPreferences.areThreadMessagesEnabled()) return false
        if (!initialState.isFromThreadTimeline) return false
        if (event.root.getClearType() != EventType.MESSAGE &&
                !event.isSticker() && !event.isPoll()) return false
        if (!actionPermissions.canSendMessage) return false

        return when (messageContent?.msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_FILE,
            MessageType.MSGTYPE_POLL_START,
            MessageType.MSGTYPE_STICKER_LOCAL -> event.root.threadDetails?.isRootThread ?: false
            else                              -> false
        }
    }

    private fun canQuote(event: TimelineEvent, messageContent: MessageContent?, actionPermissions: ActionPermissions): Boolean {
        // Only event of type EventType.MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        if (!actionPermissions.canSendMessage) return false
        return when (messageContent?.msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_LOCATION -> {
                true
            }
            else                         -> false
        }
    }

    private fun canRedact(event: TimelineEvent, actionPermissions: ActionPermissions): Boolean {
        // Only event of type EventType.MESSAGE, EventType.STICKER and EventType.POLL_START are supported for the moment
        if (event.root.getClearType() !in listOf(EventType.MESSAGE, EventType.STICKER) + EventType.POLL_START) return false
        // Message sent by the current user can always be redacted
        if (event.root.senderId == session.myUserId) return true
        // Check permission for messages sent by other users
        return actionPermissions.canRedact
    }

    private fun canRetry(event: TimelineEvent, actionPermissions: ActionPermissions): Boolean {
        return event.root.sendState.hasFailed() &&
                actionPermissions.canSendMessage &&
                (event.root.isAttachmentMessage() || event.root.isTextMessage())
    }

    private fun canViewReactions(event: TimelineEvent): Boolean {
        // Only event of type EventType.MESSAGE, EventType.STICKER and EventType.POLL_START are supported for the moment
        if (event.root.getClearType() !in listOf(EventType.MESSAGE, EventType.STICKER) + EventType.POLL_START) return false
        return event.annotations?.reactionsSummary?.isNotEmpty() ?: false
    }

    private fun canEdit(event: TimelineEvent, myUserId: String, actionPermissions: ActionPermissions): Boolean {
        // Only event of type EventType.MESSAGE and EventType.POLL_START are supported for the moment
        if (event.root.getClearType() !in listOf(EventType.MESSAGE) + EventType.POLL_START) return false
        if (!actionPermissions.canSendMessage) return false
        // TODO if user is admin or moderator
        val messageContent = event.root.getClearContent().toModel<MessageContent>()
        return event.root.senderId == myUserId && (
                messageContent?.msgType == MessageType.MSGTYPE_TEXT ||
                        messageContent?.msgType == MessageType.MSGTYPE_EMOTE ||
                        canEditPoll(event)
                )
    }

    private fun canCopy(msgType: String?): Boolean {
        return when (msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_LOCATION -> true
            else                         -> false
        }
    }

    private fun canShare(msgType: String?): Boolean {
        return when (msgType) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_LOCATION,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_FILE -> true
            else                     -> false
        }
    }

    private fun canSave(msgType: String?): Boolean {
        return when (msgType) {
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_FILE -> true
            else                     -> false
        }
    }

    private fun canEndPoll(event: TimelineEvent, actionPermissions: ActionPermissions): Boolean {
        return event.root.getClearType() in EventType.POLL_START &&
                canRedact(event, actionPermissions) &&
                event.annotations?.pollResponseSummary?.closedTime == null
    }

    private fun canEditPoll(event: TimelineEvent): Boolean {
        return event.root.getClearType() in EventType.POLL_START &&
                event.annotations?.pollResponseSummary?.closedTime == null &&
                event.annotations?.pollResponseSummary?.aggregatedContent?.totalVotes ?: 0 == 0
    }
}
