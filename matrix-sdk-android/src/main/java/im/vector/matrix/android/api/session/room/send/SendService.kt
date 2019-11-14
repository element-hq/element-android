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

package im.vector.matrix.android.api.session.room.send

import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Cancelable

/**
 * This interface defines methods to send events in a room. It's implemented at the room level.
 */
interface SendService {

    /**
     * Method to send a text message asynchronously.
     * The text to send can be a Spannable and contains special spans (UserMentionSpan) that will be translated
     * by the sdk into pills.
     * @param text the text message to send
     * @param msgType the message type: MessageType.MSGTYPE_TEXT (default) or MessageType.MSGTYPE_EMOTE
     * @param autoMarkdown If true, the SDK will generate a formatted HTML message from the body text if markdown syntax is present
     * @return a [Cancelable]
     */
    fun sendTextMessage(text: CharSequence, msgType: String = MessageType.MSGTYPE_TEXT, autoMarkdown: Boolean = false): Cancelable

    /**
     * Method to send a text message with a formatted body.
     * @param text the text message to send
     * @param formattedText The formatted body using MessageType#FORMAT_MATRIX_HTML
     * @return a [Cancelable]
     */
    fun sendFormattedTextMessage(text: String, formattedText: String, msgType: String = MessageType.MSGTYPE_TEXT): Cancelable

    /**
     * Method to send a media asynchronously.
     * @param attachment the media to send
     * @return a [Cancelable]
     */
    fun sendMedia(attachment: ContentAttachmentData): Cancelable

    /**
     * Method to send a list of media asynchronously.
     * @param attachments the list of media to send
     * @return a [Cancelable]
     */
    fun sendMedias(attachments: List<ContentAttachmentData>): Cancelable

    /**
     * Redacts (delete) the given event.
     * @param event The event to redact
     * @param reason Optional reason string
     */
    fun redactEvent(event: Event, reason: String?): Cancelable

    /**
     * Schedule this message to be resent
     * @param localEcho the unsent local echo
     */
    fun resendTextMessage(localEcho: TimelineEvent): Cancelable?

    /**
     * Schedule this message to be resent
     * @param localEcho the unsent local echo
     */
    fun resendMediaMessage(localEcho: TimelineEvent): Cancelable?

    /**
     * Remove this failed message from the timeline
     * @param localEcho the unsent local echo
     */
    fun deleteFailedEcho(localEcho: TimelineEvent)

    fun clearSendingQueue()

    /**
     * Resend all failed messages one by one (and keep order)
     */
    fun resendAllFailedMessages()
}
