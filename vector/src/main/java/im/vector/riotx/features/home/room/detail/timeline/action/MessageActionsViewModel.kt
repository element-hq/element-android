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
package im.vector.riotx.features.home.room.detail.timeline.action

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import dagger.Lazy
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.isTextMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getLastMessageContent
import im.vector.matrix.android.api.session.room.timeline.hasBeenEdited
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.rx.RxRoom
import im.vector.matrix.rx.unwrap
import im.vector.riotx.R
import im.vector.riotx.core.extensions.canReact
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotx.features.html.EventHtmlRenderer
import im.vector.riotx.features.html.VectorHtmlCompressor
import im.vector.riotx.features.settings.VectorPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Quick reactions state
 */
data class ToggleState(
        val reaction: String,
        val isSelected: Boolean
)

data class MessageActionState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val timelineEvent: Async<TimelineEvent> = Uninitialized,
        val messageBody: CharSequence? = null,
        // For quick reactions
        val quickStates: Async<List<ToggleState>> = Uninitialized,
        // For actions
        val actions: Async<List<EventSharedAction>> = Uninitialized,
        val expendedReportContentMenu: Boolean = false
) : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)

    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())

    fun senderName(): String = informationData.memberName?.toString() ?: ""

    fun time(): String? = timelineEvent()?.root?.originServerTs?.let { dateFormat.format(Date(it)) } ?: ""

    fun canReact() = timelineEvent()?.canReact() == true
}

/**
 * Information related to an event and used to display preview in contextual bottom sheet.
 */
class MessageActionsViewModel @AssistedInject constructor(@Assisted
                                                          initialState: MessageActionState,
                                                          private val eventHtmlRenderer: Lazy<EventHtmlRenderer>,
                                                          private val htmlCompressor: VectorHtmlCompressor,
                                                          private val session: Session,
                                                          private val noticeEventFormatter: NoticeEventFormatter,
                                                          private val stringProvider: StringProvider,
                                                          private val vectorPreferences: VectorPreferences
) : VectorViewModel<MessageActionState, MessageActionsAction>(initialState) {

    private val eventId = initialState.eventId
    private val informationData = initialState.informationData
    private val room = session.getRoom(initialState.roomId)

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: MessageActionState): MessageActionsViewModel
    }

    companion object : MvRxViewModelFactory<MessageActionsViewModel, MessageActionState> {

        val quickEmojis = listOf("👍", "👎", "😄", "🎉", "😕", "❤️", "🚀", "👀")

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: MessageActionState): MessageActionsViewModel? {
            val fragment: MessageActionsBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.messageActionViewModelFactory.create(state)
        }
    }

    init {
        observeEvent()
        observeReactions()
        observeEventAction()
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

    private fun observeEvent() {
        if (room == null) return
        RxRoom(room)
                .liveTimelineEvent(eventId)
                .unwrap()
                .execute {
                    copy(
                            timelineEvent = it,
                            messageBody = computeMessageBody(it)
                    )
                }
    }

    private fun observeEventAction() {
        if (room == null) return
        RxRoom(room)
                .liveTimelineEvent(eventId)
                .map {
                    actionsForEvent(it)
                }
                .execute {
                    copy(actions = it)
                }
    }

    private fun observeReactions() {
        if (room == null) return
        RxRoom(room)
                .liveAnnotationSummary(eventId)
                .map { annotations ->
                    quickEmojis.map { emoji ->
                        ToggleState(emoji, annotations.getOrNull()?.reactionsSummary?.firstOrNull { it.key == emoji }?.addedByMe ?: false)
                    }
                }
                .execute {
                    copy(quickStates = it)
                }
    }

    private fun computeMessageBody(timelineEvent: Async<TimelineEvent>): CharSequence? {
        return when (timelineEvent()?.root?.getClearType()) {
            EventType.MESSAGE,
            EventType.STICKER     -> {
                val messageContent: MessageContent? = timelineEvent()?.getLastMessageContent()
                if (messageContent is MessageTextContent && messageContent.format == MessageType.FORMAT_MATRIX_HTML) {
                    val html = messageContent.formattedBody
                            ?.takeIf { it.isNotBlank() }
                            ?.let { htmlCompressor.compress(it) }
                            ?: messageContent.body

                    eventHtmlRenderer.get().render(html)
                } else {
                    messageContent?.body
                }
            }
            EventType.STATE_ROOM_NAME,
            EventType.STATE_ROOM_TOPIC,
            EventType.STATE_ROOM_MEMBER,
            EventType.STATE_ROOM_ALIASES,
            EventType.STATE_ROOM_CANONICAL_ALIAS,
            EventType.STATE_ROOM_HISTORY_VISIBILITY,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER -> {
                timelineEvent()?.let { noticeEventFormatter.format(it) }
            }
            else                  -> null
        }
    }

    private fun actionsForEvent(optionalEvent: Optional<TimelineEvent>): List<EventSharedAction> {
        val event = optionalEvent.getOrNull() ?: return emptyList()

        val messageContent: MessageContent? = event.annotations?.editSummary?.aggregatedContent.toModel()
                ?: event.root.getClearContent().toModel()
        val type = messageContent?.type

        return arrayListOf<EventSharedAction>().apply {
            if (event.root.sendState.hasFailed()) {
                if (canRetry(event)) {
                    add(EventSharedAction.Resend(eventId))
                }
                add(EventSharedAction.Remove(eventId))
            } else if (event.root.sendState.isSending()) {
                // TODO is uploading attachment?
                if (canCancel(event)) {
                    add(EventSharedAction.Cancel(eventId))
                }
            } else if (event.root.sendState == SendState.SYNCED) {
                if (!event.root.isRedacted()) {
                    if (canReply(event, messageContent)) {
                        add(EventSharedAction.Reply(eventId))
                    }

                    if (canEdit(event, session.myUserId)) {
                        add(EventSharedAction.Edit(eventId))
                    }

                    if (canRedact(event, session.myUserId)) {
                        add(EventSharedAction.Delete(eventId))
                    }

                    if (canCopy(type)) {
                        // TODO copy images? html? see ClipBoard
                        add(EventSharedAction.Copy(messageContent!!.body))
                    }

                    if (event.canReact()) {
                        add(EventSharedAction.AddReaction(eventId))
                    }

                    if (canQuote(event, messageContent)) {
                        add(EventSharedAction.Quote(eventId))
                    }

                    if (canViewReactions(event)) {
                        add(EventSharedAction.ViewReactions(informationData))
                    }

                    if (event.hasBeenEdited()) {
                        add(EventSharedAction.ViewEditHistory(informationData))
                    }

                    if (canShare(type)) {
                        if (messageContent is MessageImageContent) {
                            session.contentUrlResolver().resolveFullSize(messageContent.url)?.let { url ->
                                add(EventSharedAction.Share(url))
                            }
                        }
                        // TODO
                    }

                    if (event.root.sendState == SendState.SENT) {
                        // TODO Can be redacted

                        // TODO sent by me or sufficient power level
                    }
                }

                if (vectorPreferences.developerMode()) {
                    add(EventSharedAction.ViewSource(event.root.toContentStringWithIndent()))
                    if (event.isEncrypted()) {
                        val decryptedContent = event.root.toClearContentStringWithIndent()
                                ?: stringProvider.getString(R.string.encryption_information_decryption_error)
                        add(EventSharedAction.ViewDecryptedSource(decryptedContent))
                    }
                }

                add(EventSharedAction.CopyPermalink(eventId))

                if (session.myUserId != event.root.senderId) {
                    // not sent by me
                    if (event.root.getClearType() == EventType.MESSAGE) {
                        add(EventSharedAction.ReportContent(eventId, event.root.senderId))
                    }

                    add(EventSharedAction.Separator)
                    add(EventSharedAction.IgnoreUser(event.root.senderId))
                }
            }
        }
    }

    private fun canCancel(@Suppress("UNUSED_PARAMETER") event: TimelineEvent): Boolean {
        return false
    }

    private fun canReply(event: TimelineEvent, messageContent: MessageContent?): Boolean {
        // Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        return when (messageContent?.type) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_VIDEO,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_FILE -> true
            else                     -> false
        }
    }

    private fun canQuote(event: TimelineEvent, messageContent: MessageContent?): Boolean {
        // Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        return when (messageContent?.type) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.FORMAT_MATRIX_HTML,
            MessageType.MSGTYPE_LOCATION -> {
                true
            }
            else                         -> false
        }
    }

    private fun canRedact(event: TimelineEvent, myUserId: String): Boolean {
        // Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        // TODO if user is admin or moderator
        return event.root.senderId == myUserId
    }

    private fun canRetry(event: TimelineEvent): Boolean {
        return event.root.sendState.hasFailed() && event.root.isTextMessage()
    }

    private fun canViewReactions(event: TimelineEvent): Boolean {
        // Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        // TODO if user is admin or moderator
        return event.annotations?.reactionsSummary?.isNotEmpty() ?: false
    }

    private fun canEdit(event: TimelineEvent, myUserId: String): Boolean {
        // Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        // TODO if user is admin or moderator
        val messageContent = event.root.getClearContent().toModel<MessageContent>()
        return event.root.senderId == myUserId && (
                messageContent?.type == MessageType.MSGTYPE_TEXT
                        || messageContent?.type == MessageType.MSGTYPE_EMOTE
                )
    }

    private fun canCopy(type: String?): Boolean {
        return when (type) {
            MessageType.MSGTYPE_TEXT,
            MessageType.MSGTYPE_NOTICE,
            MessageType.MSGTYPE_EMOTE,
            MessageType.FORMAT_MATRIX_HTML,
            MessageType.MSGTYPE_LOCATION -> true
            else                         -> false
        }
    }

    private fun canShare(type: String?): Boolean {
        return when (type) {
            MessageType.MSGTYPE_IMAGE,
            MessageType.MSGTYPE_AUDIO,
            MessageType.MSGTYPE_VIDEO -> true
            else                      -> false
        }
    }
}
