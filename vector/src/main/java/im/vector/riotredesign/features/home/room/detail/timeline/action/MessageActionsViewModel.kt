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
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.riotredesign.core.platform.VectorViewModel
import org.commonmark.parser.Parser
import org.koin.android.ext.android.get
import ru.noties.markwon.Markwon
import ru.noties.markwon.html.HtmlPlugin
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


data class MessageActionState(
        val userId: String = "",
        val senderName: String = "",
        val messageBody: CharSequence? = null,
        val ts: String? = null,
        val showPreview: Boolean = false,
        val canReact: Boolean = false,
        val senderAvatarPath: String? = null)
    : MvRxState

/**
 * Information related to an event and used to display preview in contextual bottomsheet.
 */
class MessageActionsViewModel(initialState: MessageActionState) : VectorViewModel<MessageActionState>(initialState) {

    companion object : MvRxViewModelFactory<MessageActionsViewModel, MessageActionState> {

        override fun initialState(viewModelContext: ViewModelContext): MessageActionState? {
            val currentSession = viewModelContext.activity.get<Session>()
            val parcel = viewModelContext.args as TimelineEventFragmentArgs

            val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())

            val event = currentSession.getRoom(parcel.roomId)?.getTimeLineEvent(parcel.eventId)
            return if (event != null) {
                val messageContent: MessageContent? = event.annotations?.editSummary?.aggregatedContent?.toModel()
                        ?: event.root.content.toModel()
                val originTs = event.root.originServerTs
                var body: CharSequence? = messageContent?.body
                if (messageContent is MessageTextContent && messageContent.format == MessageType.FORMAT_MATRIX_HTML) {
                    val parser = Parser.builder().build()
                    val document = parser.parse(messageContent.formattedBody ?: messageContent.body)
                    body = Markwon.builder(viewModelContext.activity)
                            .usePlugin(HtmlPlugin.create()).build().render(document)
                }
                MessageActionState(
                        userId = event.root.sender ?: "",
                        senderName = parcel.informationData.memberName.toString(),
                        messageBody = body,
                        ts = dateFormat.format(Date(originTs ?: 0)),
                        showPreview = event.root.type == EventType.MESSAGE,
                        canReact = event.root.type == EventType.MESSAGE,
                        senderAvatarPath = currentSession.contentUrlResolver().resolveFullSize(parcel.informationData.avatarUrl)
                )
            } else {
                //can this happen?
                Timber.e("Failed to retrieve event")
                null
            }
        }
    }
}