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

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import dagger.Lazy
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.rx.RxRoom
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
        val timelineEvent: Async<TimelineEvent> = Uninitialized
) : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)


    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())

    fun senderName(): String = informationData.memberName?.toString() ?: ""

    fun time(): String? = timelineEvent()?.root?.originServerTs?.let { dateFormat.format(Date(it)) }
            ?: ""

    fun canReact(): Boolean = timelineEvent()?.root?.type == EventType.MESSAGE && timelineEvent()?.sendState?.isSent() == true

    fun messageBody(eventHtmlRenderer: EventHtmlRenderer?, noticeEventFormatter: NoticeEventFormatter?): CharSequence? {
        return when (timelineEvent()?.root?.getClearType()) {
            EventType.MESSAGE     -> {
                val messageContent: MessageContent? = timelineEvent()?.annotations?.editSummary?.aggregatedContent?.toModel()
                        ?: timelineEvent()?.root?.getClearContent().toModel()
                if (messageContent is MessageTextContent && messageContent.format == MessageType.FORMAT_MATRIX_HTML) {
                    eventHtmlRenderer?.render(messageContent.formattedBody
                            ?: messageContent.body)
                } else {
                    messageContent?.body
                }
            }
            EventType.STATE_ROOM_NAME,
            EventType.STATE_ROOM_TOPIC,
            EventType.STATE_ROOM_MEMBER,
            EventType.STATE_HISTORY_VISIBILITY,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER -> {
                timelineEvent()?.let { noticeEventFormatter?.format(it) }
            }
            else                  -> null
        }
    }
}

/**
 * Information related to an event and used to display preview in contextual bottomsheet.
 */
class MessageActionsViewModel @AssistedInject constructor(@Assisted
                                                          initialState: MessageActionState,
                                                          private val eventHtmlRenderer: Lazy<EventHtmlRenderer>,
                                                          session: Session,
                                                          private val noticeEventFormatter: NoticeEventFormatter
) : VectorViewModel<MessageActionState>(initialState) {


    private val eventId = initialState.eventId
    private val room = session.getRoom(initialState.roomId)

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
        observeEvent()
    }

    private fun observeEvent() {
        if (room == null) return
        RxRoom(room)
                .liveTimelineEvent(eventId)
                .execute {
                    copy(timelineEvent = it)
                }
    }

    fun resolveBody(state: MessageActionState): CharSequence? {
        return state.messageBody(eventHtmlRenderer.get(), noticeEventFormatter)
    }

}