/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.send

import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable

/**
 * This interface defines methods to send events in a room. It's implemented at the room level.
 */
interface SendService {

    /**
     * Method to send a generic event asynchronously. If you want to send a state event, please use [StateService] instead.
     * @param eventType the type of the event
     * @param content the optional body as a json dict.
     * @return a [Cancelable]
     */
    fun sendEvent(eventType: String, content: Content?): Cancelable

    /**
     * Method to send a text message asynchronously.
     * The text to send can be a Spannable and contains special spans (MatrixItemSpan) that will be translated
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
     * @param msgType the message type: MessageType.MSGTYPE_TEXT (default) or MessageType.MSGTYPE_EMOTE
     * @return a [Cancelable]
     */
    fun sendFormattedTextMessage(text: String, formattedText: String, msgType: String = MessageType.MSGTYPE_TEXT): Cancelable

    /**
     * Method to send a media asynchronously.
     * @param attachment the media to send
     * @param compressBeforeSending set to true to compress images before sending them
     * @param roomIds set of roomIds to where the media will be sent. The current roomId will be add to this set if not present.
     *                It can be useful to send media to multiple room. It's safe to include the current roomId in this set
     * @return a [Cancelable]
     */
    fun sendMedia(attachment: ContentAttachmentData,
                  compressBeforeSending: Boolean,
                  roomIds: Set<String>): Cancelable

    /**
     * Method to send a list of media asynchronously.
     * @param attachments the list of media to send
     * @param compressBeforeSending set to true to compress images before sending them
     * @param roomIds set of roomIds to where the media will be sent. The current roomId will be add to this set if not present.
     *                It can be useful to send media to multiple room. It's safe to include the current roomId in this set
     * @return a [Cancelable]
     */
    fun sendMedias(attachments: List<ContentAttachmentData>,
                   compressBeforeSending: Boolean,
                   roomIds: Set<String>): Cancelable

    /**
     * Send a poll to the room.
     * @param question the question
     * @param options list of options
     * @return a [Cancelable]
     */
    fun sendPoll(question: String, options: List<String>): Cancelable

    /**
     * Method to send a poll response.
     * @param pollEventId the poll currently replied to
     * @param optionIndex The reply index
     * @param optionValue The option value (for compatibility)
     * @return a [Cancelable]
     */
    fun sendOptionsReply(pollEventId: String, optionIndex: Int, optionValue: String): Cancelable

    /**
     * Redact (delete) the given event.
     * @param event The event to redact
     * @param reason Optional reason string
     */
    fun redactEvent(event: Event, reason: String?): Cancelable

    /**
     * Schedule this message to be resent
     * @param localEcho the unsent local echo
     */
    fun resendTextMessage(localEcho: TimelineEvent): Cancelable

    /**
     * Schedule this message to be resent
     * @param localEcho the unsent local echo
     */
    fun resendMediaMessage(localEcho: TimelineEvent): Cancelable

    /**
     * Remove this failed message from the timeline
     * @param localEcho the unsent local echo
     */
    fun deleteFailedEcho(localEcho: TimelineEvent)

    /**
     * Cancel sending a specific event. It has to be in one of the sending states
     */
    fun cancelSend(eventId: String)

    /**
     * Resend all failed messages one by one (and keep order)
     */
    fun resendAllFailedMessages()

    /**
     * Cancel all failed messages
     */
    fun cancelAllFailedMessages()
}
