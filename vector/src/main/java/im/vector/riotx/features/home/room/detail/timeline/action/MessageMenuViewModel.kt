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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.isTextMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.hasBeenEdited
import im.vector.matrix.rx.RxRoom
import im.vector.riotx.R
import im.vector.riotx.core.extensions.canReact
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData


sealed class SimpleAction(@StringRes val titleRes: Int, @DrawableRes val iconResId: Int) {
    data class AddReaction(val eventId: String) : SimpleAction(R.string.message_add_reaction, R.drawable.ic_add_reaction)
    data class Copy(val content: String) : SimpleAction(R.string.copy, R.drawable.ic_copy)
    data class Edit(val eventId: String) : SimpleAction(R.string.edit, R.drawable.ic_edit)
    data class Quote(val eventId: String) : SimpleAction(R.string.quote, R.drawable.ic_quote)
    data class Reply(val eventId: String) : SimpleAction(R.string.reply, R.drawable.ic_reply)
    data class Share(val imageUrl: String?) : SimpleAction(R.string.share, R.drawable.ic_share)
    data class Resend(val eventId: String) : SimpleAction(R.string.global_retry, R.drawable.ic_refresh_cw)
    data class Remove(val eventId: String) : SimpleAction(R.string.remove, R.drawable.ic_trash)
    data class Delete(val eventId: String) : SimpleAction(R.string.delete, R.drawable.ic_delete)
    data class Cancel(val eventId: String) : SimpleAction(R.string.cancel, R.drawable.ic_close_round)
    data class ViewSource(val content: String) : SimpleAction(R.string.view_source, R.drawable.ic_view_source)
    data class ViewDecryptedSource(val content: String) : SimpleAction(R.string.view_decrypted_source, R.drawable.ic_view_source)
    data class CopyPermalink(val eventId: String) : SimpleAction(R.string.permalink, R.drawable.ic_permalink)
    data class Flag(val eventId: String) : SimpleAction(R.string.report_content, R.drawable.ic_flag)
    data class QuickReact(val eventId: String, val clickedOn: String, val add: Boolean) : SimpleAction(0, 0)
    data class ViewReactions(val messageInformationData: MessageInformationData) : SimpleAction(R.string.message_view_reaction, R.drawable.ic_view_reactions)
    data class ViewEditHistory(val messageInformationData: MessageInformationData) :
            SimpleAction(R.string.message_view_edit_history, R.drawable.ic_view_edit_history)
}

data class MessageMenuState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val actions: Async<List<SimpleAction>> = Uninitialized
) : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)

}

/**
 * Manages list actions for a given message (copy / paste / forward...)
 */
class MessageMenuViewModel @AssistedInject constructor(@Assisted initialState: MessageMenuState,
                                                       private val session: Session,
                                                       private val stringProvider: StringProvider) : VectorViewModel<MessageMenuState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: MessageMenuState): MessageMenuViewModel
    }

    private val room = session.getRoom(initialState.roomId)
            ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    private val eventId = initialState.eventId
    private val informationData: MessageInformationData = initialState.informationData

    companion object : MvRxViewModelFactory<MessageMenuViewModel, MessageMenuState> {
        override fun create(viewModelContext: ViewModelContext, state: MessageMenuState): MessageMenuViewModel? {
            val fragment: MessageMenuFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.messageMenuViewModelFactory.create(state)
        }
    }

    init {
        observeEvent()
    }

    private fun observeEvent() {
        RxRoom(room)
                .liveTimelineEvent(eventId)
                .map {
                    actionsForEvent(it)
                }
                .execute {
                    copy(actions = it)
                }
    }

    private fun actionsForEvent(event: TimelineEvent): List<SimpleAction> {
        val messageContent: MessageContent? = event.annotations?.editSummary?.aggregatedContent.toModel()
                ?: event.root.getClearContent().toModel()
        val type = messageContent?.type

        return arrayListOf<SimpleAction>().apply {
            if (event.root.sendState.hasFailed()) {
                if (canRetry(event)) {
                    add(SimpleAction.Resend(eventId))
                }
                add(SimpleAction.Remove(eventId))
            } else if (event.root.sendState.isSending()) {
                //TODO is uploading attachment?
                if (canCancel(event)) {
                    add(SimpleAction.Cancel(eventId))
                }
            } else {
                if (!event.root.isRedacted()) {
                    if (canReply(event, messageContent)) {
                        add(SimpleAction.Reply(eventId))
                    }

                    if (canEdit(event, session.myUserId)) {
                        add(SimpleAction.Edit(eventId))
                    }

                    if (canRedact(event, session.myUserId)) {
                        add(SimpleAction.Delete(eventId))
                    }

                    if (canCopy(type)) {
                        //TODO copy images? html? see ClipBoard
                        add(SimpleAction.Copy(messageContent!!.body))
                    }

                    if (event.canReact()) {
                        add(SimpleAction.AddReaction(eventId))
                    }

                    if (canQuote(event, messageContent)) {
                        add(SimpleAction.Quote(eventId))
                    }

                    if (canViewReactions(event)) {
                        add(SimpleAction.ViewReactions(informationData))
                    }

                    if (event.hasBeenEdited()) {
                        add(SimpleAction.ViewEditHistory(informationData))
                    }

                    if (canShare(type)) {
                        if (messageContent is MessageImageContent) {
                            add(SimpleAction.Share(session.contentUrlResolver().resolveFullSize(messageContent.url)))
                        }
                        //TODO
                    }


                    if (event.root.sendState == SendState.SENT) {

                        //TODO Can be redacted

                        //TODO sent by me or sufficient power level
                    }
                }

                add(SimpleAction.ViewSource(event.root.toContentStringWithIndent()))
                if (event.isEncrypted()) {
                    val decryptedContent = event.root.toClearContentStringWithIndent()
                            ?: stringProvider.getString(R.string.encryption_information_decryption_error)
                    add(SimpleAction.ViewDecryptedSource(decryptedContent))
                }
                add(SimpleAction.CopyPermalink(eventId))

                if (session.myUserId != event.root.senderId && event.root.getClearType() == EventType.MESSAGE) {
                    //not sent by me
                    add(SimpleAction.Flag(eventId))
                }
            }
        }
    }

    private fun canCancel(event: TimelineEvent): Boolean {
        return false
    }

    private fun canReply(event: TimelineEvent, messageContent: MessageContent?): Boolean {
        //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
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
        //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
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
        //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        //TODO if user is admin or moderator
        return event.root.senderId == myUserId
    }

    private fun canRetry(event: TimelineEvent): Boolean {
        return event.root.sendState.hasFailed() && event.root.isTextMessage()
    }


    private fun canViewReactions(event: TimelineEvent): Boolean {
        //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        //TODO if user is admin or moderator
        return event.annotations?.reactionsSummary?.isNotEmpty() ?: false
    }


    private fun canEdit(event: TimelineEvent, myUserId: String): Boolean {
        //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
        if (event.root.getClearType() != EventType.MESSAGE) return false
        //TODO if user is admin or moderator
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
