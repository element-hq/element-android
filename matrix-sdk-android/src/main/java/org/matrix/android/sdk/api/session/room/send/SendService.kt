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
import org.matrix.android.sdk.api.session.room.model.message.PollType
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
     * Method to quote an events content.
     * @param quotedEvent The event to which we will quote it's content.
     * @param text the text message to send
     * @param autoMarkdown If true, the SDK will generate a formatted HTML message from the body text if markdown syntax is present
     * @return a [Cancelable]
     */
    fun sendQuotedTextMessage(quotedEvent: TimelineEvent, text: String, autoMarkdown: Boolean, rootThreadEventId: String? = null): Cancelable

    /**
     * Method to send a media asynchronously.
     * @param attachment the media to send
     * @param compressBeforeSending set to true to compress images before sending them
     * @param roomIds set of roomIds to where the media will be sent. The current roomId will be add to this set if not present.
     *                It can be useful to send media to multiple room. It's safe to include the current roomId in this set
     * @param rootThreadEventId when this param is not null, the Media will be sent in this specific thread
     * @return a [Cancelable]
     */
    fun sendMedia(attachment: ContentAttachmentData,
                  compressBeforeSending: Boolean,
                  roomIds: Set<String>,
                  rootThreadEventId: String? = null): Cancelable

    /**
     * Method to send a list of media asynchronously.
     * @param attachments the list of media to send
     * @param compressBeforeSending set to true to compress images before sending them
     * @param roomIds set of roomIds to where the media will be sent. The current roomId will be add to this set if not present.
     *                It can be useful to send media to multiple room. It's safe to include the current roomId in this set
     * @param rootThreadEventId when this param is not null, all the Media will be sent in this specific thread
     * @return a [Cancelable]
     */
    fun sendMedias(attachments: List<ContentAttachmentData>,
                   compressBeforeSending: Boolean,
                   roomIds: Set<String>,
                   rootThreadEventId: String? = null): Cancelable

    /**
     * Send a poll to the room.
     * @param pollType indicates open or closed polls
     * @param question the question
     * @param options list of options
     * @return a [Cancelable]
     */
    fun sendPoll(pollType: PollType, question: String, options: List<String>): Cancelable

    /**
     * Method to send a poll response.
     * @param pollEventId the poll currently replied to
     * @param answerId The id of the answer
     * @return a [Cancelable]
     */
    fun voteToPoll(pollEventId: String, answerId: String): Cancelable

    /**
     * End a poll in the room.
     * @param pollEventId event id of the poll
     * @return a [Cancelable]
     */
    fun endPoll(pollEventId: String): Cancelable

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
     * Send a location event to the room
     * @param latitude required latitude of the location
     * @param longitude required longitude of the location
     * @param uncertainty Accuracy of the location in meters
     * @param isUserLocation indicates whether the location data corresponds to the user location or not
     */
    fun sendLocation(latitude: Double, longitude: Double, uncertainty: Double?, isUserLocation: Boolean): Cancelable

    /**
     * Send a live location event to the room. beacon_info state event has to be sent before sending live location updates.
     * @param beaconInfoEventId event id of the initial beacon info state event
     * @param latitude required latitude of the location
     * @param longitude required longitude of the location
     * @param uncertainty Accuracy of the location in meters
     */
    fun sendLiveLocation(beaconInfoEventId: String, latitude: Double, longitude: Double, uncertainty: Double?): Cancelable

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
