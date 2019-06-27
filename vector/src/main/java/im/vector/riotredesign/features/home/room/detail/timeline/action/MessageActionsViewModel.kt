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

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.html.EventHtmlRenderer
import java.text.SimpleDateFormat
import java.util.*


data class MessageActionState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val userId: String = "",
        val senderName: String = "",
        val messageBody: CharSequence? = null,
        val ts: String? = null,
        val showPreview: Boolean = false,
        val canReact: Boolean = false,
        val senderAvatarPath: String? = null)
    : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)

}

/**
 * Information related to an event and used to display preview in contextual bottomsheet.
 */
class MessageActionsViewModel @AssistedInject constructor(@Assisted
                                                          initialState: MessageActionState,
                                                          private val eventHtmlRenderer: EventHtmlRenderer,
                                                          private val session: Session,
                                                          private val noticeEventFormatter: NoticeEventFormatter
) : VectorViewModel<MessageActionState>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val informationData = initialState.informationData

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: MessageActionState): MessageActionsViewModel
    }

    companion object : MvRxViewModelFactory<MessageActionsViewModel, MessageActionState> {

        override fun create(viewModelContext: ViewModelContext, state: MessageActionState): MessageActionsViewModel? {
            val fragment: MessageActionsBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.messageActionViewModelFactory.create(state)
        }
    }


    init {
        setState { reduceState(this) }
    }

    private fun reduceState(state: MessageActionState): MessageActionState {
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
        val event = session.getRoom(roomId)?.getTimeLineEvent(eventId) ?: return state
        var body: CharSequence? = null
        val originTs = event.root.originServerTs
        when (event.root.getClearType()) {
            EventType.MESSAGE     -> {
                val messageContent: MessageContent? = event.annotations?.editSummary?.aggregatedContent?.toModel()
                                                      ?: event.root.getClearContent().toModel()
                body = messageContent?.body
                if (messageContent is MessageTextContent && messageContent.format == MessageType.FORMAT_MATRIX_HTML) {
                    body = eventHtmlRenderer.render(messageContent.formattedBody
                                                    ?: messageContent.body)
                }
            }
            EventType.STATE_ROOM_NAME,
            EventType.STATE_ROOM_TOPIC,
            EventType.STATE_ROOM_MEMBER,
            EventType.STATE_HISTORY_VISIBILITY,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER -> {
                body = noticeEventFormatter.format(event)
            }
        }
        return state.copy(
                userId = event.root.senderId ?: "",
                senderName = informationData.memberName?.toString() ?: "",
                messageBody = body,
                ts = dateFormat.format(Date(originTs ?: 0)),
                showPreview = body != null,
                canReact = event.root.type == EventType.MESSAGE && event.sendState.isSent(),
                senderAvatarPath = informationData.avatarUrl
        )
    }

}