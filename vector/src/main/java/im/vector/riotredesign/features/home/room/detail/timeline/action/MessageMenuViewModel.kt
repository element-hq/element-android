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
package im.vector.riotredesign.features.home.room.detail.timeline.action

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorViewModel
import org.json.JSONObject
import org.koin.android.ext.android.get

data class SimpleAction(val uid: String, val titleRes: Int, val iconResId: Int?, val data: Any? = null)

data class MessageMenuState(val actions: List<SimpleAction> = emptyList()) : MvRxState

/**
 * Manages list actions for a given message (copy / paste / forward...)
 */
class MessageMenuViewModel(initialState: MessageMenuState) : VectorViewModel<MessageMenuState>(initialState) {

    companion object : MvRxViewModelFactory<MessageMenuViewModel, MessageMenuState> {

        override fun initialState(viewModelContext: ViewModelContext): MessageMenuState? {
            // Args are accessible from the context.
            val currentSession = viewModelContext.activity.get<Session>()
            val parcel = viewModelContext.args as TimelineEventFragmentArgs
            val event = currentSession.getRoom(parcel.roomId)?.getTimeLineEvent(parcel.eventId)
                    ?: return null

            val messageContent: MessageContent? = event.annotations?.editSummary?.aggregatedContent?.toModel()
                    ?: event.root.content.toModel()
            val type = messageContent?.type

            if (!event.sendState.isSent()) {
                //Resend and Delete
                return MessageMenuState(
                        //TODO
                        listOf(
//                                SimpleAction(ACTION_RESEND, R.string.resend, R.drawable.ic_send, event.root.eventId),
//                                //TODO delete icon
//                                SimpleAction(ACTION_DELETE, R.string.delete, R.drawable.ic_delete, event.root.eventId)
                        )
                )
            }

            val actions = ArrayList<SimpleAction>().apply {

                if (event.sendState == SendState.SENDING) {
                    //TODO add cancel?
                    return@apply
                }
                //TODO is downloading attachement?

                if (canReact(event, messageContent)) {
                    this.add(SimpleAction(ACTION_ADD_REACTION, R.string.message_add_reaction, R.drawable.ic_add_reaction, event.root.eventId))
                }
                if (canCopy(type)) {
                    //TODO copy images? html? see ClipBoard
                    this.add(SimpleAction(ACTION_COPY, R.string.copy, R.drawable.ic_copy, messageContent!!.body))
                }

                if (canReply(event, messageContent)) {
                    this.add(SimpleAction(ACTION_REPLY, R.string.reply, R.drawable.ic_reply, event.root.eventId))
                }

                if (canEdit(event, currentSession.sessionParams.credentials.userId)) {
                    this.add(SimpleAction(ACTION_EDIT, R.string.edit, R.drawable.ic_edit, event.root.eventId))
                }

                if (canRedact(event, currentSession.sessionParams.credentials.userId)) {
                    this.add(SimpleAction(ACTION_DELETE, R.string.delete, R.drawable.ic_delete, event.root.eventId))
                }

                if (canQuote(event, messageContent)) {
                    this.add(SimpleAction(ACTION_QUOTE, R.string.quote, R.drawable.ic_quote, parcel.eventId))
                }

                if (canViewReactions(event)) {
                    this.add(SimpleAction(ACTION_VIEW_REACTIONS, R.string.message_view_reaction, R.drawable.ic_view_reactions, parcel.informationData))
                }

                if (canShare(type)) {
                    if (messageContent is MessageImageContent) {
                        this.add(
                                SimpleAction(ACTION_SHARE,
                                        R.string.share, R.drawable.ic_share,
                                        currentSession.contentUrlResolver().resolveFullSize(messageContent.url))
                        )
                    }
                    //TODO
                }


                if (event.sendState == SendState.SENT) {

                    //TODO Can be redacted

                    //TODO sent by me or sufficient power level
                }


                this.add(SimpleAction(VIEW_SOURCE, R.string.view_source, R.drawable.ic_view_source, JSONObject(event.root.toContent()).toString(4)))
                if (event.isEncrypted()) {
                    this.add(SimpleAction(VIEW_DECRYPTED_SOURCE, R.string.view_decrypted_source, R.drawable.ic_view_source, parcel.eventId))
                }
                this.add(SimpleAction(ACTION_COPY_PERMALINK, R.string.permalink, R.drawable.ic_permalink, parcel.eventId))

                if (currentSession.sessionParams.credentials.userId != event.root.sender && event.root.type == EventType.MESSAGE) {
                    //not sent by me
                    this.add(SimpleAction(ACTION_FLAG, R.string.report_content, R.drawable.ic_flag, parcel.eventId))
                }
            }

            return MessageMenuState(actions)
        }

        private fun canReply(event: TimelineEvent, messageContent: MessageContent?): Boolean {
            //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
            if (event.root.type != EventType.MESSAGE) return false
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

        private fun canReact(event: TimelineEvent, messageContent: MessageContent?): Boolean {
            //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
            return event.root.type == EventType.MESSAGE
        }

        private fun canQuote(event: TimelineEvent, messageContent: MessageContent?): Boolean {
            //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
            if (event.root.type != EventType.MESSAGE) return false
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
            if (event.root.type != EventType.MESSAGE) return false
            //TODO if user is admin or moderator
            return event.root.sender == myUserId
        }

        private fun canViewReactions(event: TimelineEvent): Boolean {
            //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
            if (event.root.type != EventType.MESSAGE) return false
            //TODO if user is admin or moderator
            return event.annotations?.reactionsSummary?.isNotEmpty() ?: false
        }

        private fun canEdit(event: TimelineEvent, myUserId: String): Boolean {
            //Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment
            if (event.root.type != EventType.MESSAGE) return false
            //TODO if user is admin or moderator
            val messageContent = event.root.content.toModel<MessageContent>()
            return event.root.sender == myUserId && (
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
                MessageType.MSGTYPE_LOCATION -> {
                    true
                }
                else                         -> false
            }
        }


        private fun canShare(type: String?): Boolean {
            return when (type) {
                MessageType.MSGTYPE_IMAGE,
                MessageType.MSGTYPE_AUDIO,
                MessageType.MSGTYPE_VIDEO -> {
                    true
                }
                else                      -> false
            }
        }

        const val ACTION_ADD_REACTION = "add_reaction"
        const val ACTION_COPY = "copy"
        const val ACTION_EDIT = "edit"
        const val ACTION_QUOTE = "quote"
        const val ACTION_REPLY = "reply"
        const val ACTION_SHARE = "share"
        const val ACTION_RESEND = "resend"
        const val ACTION_DELETE = "delete"
        const val VIEW_SOURCE = "VIEW_SOURCE"
        const val VIEW_DECRYPTED_SOURCE = "VIEW_DECRYPTED_SOURCE"
        const val ACTION_COPY_PERMALINK = "ACTION_COPY_PERMALINK"
        const val ACTION_FLAG = "ACTION_FLAG"
        const val ACTION_QUICK_REACT = "ACTION_QUICK_REACT"
        const val ACTION_VIEW_REACTIONS = "ACTION_VIEW_REACTIONS"


    }
}
