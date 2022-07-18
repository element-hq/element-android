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
package org.matrix.android.sdk.api.session.room.model.relation

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional

/**
 * In some cases, events may wish to reference other events.
 * This could be to form a thread of messages for the user to follow along with,
 * or to provide more context as to what a particular event is describing.
 * Relation are used to associate new information with an existing event.
 *
 * Relations are events which have an m.relates_to mixin in their contents,
 * and the new information they convey is expressed in their usual event type and content.
 *
 * Three types of relations are defined, each defining different behaviour when aggregated:
 *
 *  m.annotation - lets you define an event which annotates an existing event.
 *                  When aggregated, groups events together based on key and returns a count.
 *                  (aka SQL's COUNT) These are primarily intended for handling reactions.
 *
 *  m.replace - lets you define an event which replaces an existing event.
 *             When aggregated, returns the most recent replacement event. (aka SQL's MAX)
 *             These are primarily intended for handling edits.
 *
 *  m.reference - lets you define an event which references an existing event.
 *              When aggregated, currently doesn't do anything special, but in future could bundle chains of references (i.e. threads).
 *              These are primarily intended for handling replies (and in future threads).
 *
 *  m.thread - lets you define an event which is a thread reply to an existing event.
 *             When aggregated, returns the most thread event
 */
interface RelationService {

    /**
     * Sends a reaction (emoji) to the targetedEvent.
     * It has no effect if the user has already added the same reaction to the event.
     * @param targetEventId the id of the event being reacted
     * @param reaction the reaction (preferably emoji)
     */
    fun sendReaction(
            targetEventId: String,
            reaction: String
    ): Cancelable

    /**
     * Undo a reaction (emoji) to the targetedEvent.
     * @param targetEventId the id of the event being reacted
     * @param reaction the reaction (preferably emoji)
     */
    suspend fun undoReaction(
            targetEventId: String,
            reaction: String
    ): Cancelable

    /**
     * Edit a poll.
     * @param targetEvent The poll event to edit
     * @param pollType indicates open or closed polls
     * @param question The edited question
     * @param options The edited options
     */
    fun editPoll(
            targetEvent: TimelineEvent,
            pollType: PollType,
            question: String,
            options: List<String>
    ): Cancelable

    /**
     * Edit a text message body. Limited to "m.text" contentType.
     * @param targetEvent The event to edit
     * @param msgType the message type
     * @param newBodyText The edited body
     * @param newBodyAutoMarkdown true to parse markdown on the new body
     * @param compatibilityBodyText The text that will appear on clients that don't support yet edition
     */
    fun editTextMessage(
            targetEvent: TimelineEvent,
            msgType: String,
            newBodyText: CharSequence,
            newBodyAutoMarkdown: Boolean,
            compatibilityBodyText: String = "* $newBodyText"
    ): Cancelable

    /**
     * Edit a reply. This is a special case because replies contains fallback text as a prefix.
     * This method will take the new body (stripped from fallbacks) and re-add them before sending.
     * @param replyToEdit The event to edit
     * @param originalTimelineEvent the message that this reply (being edited) is relating to
     * @param newBodyText The edited body (stripped from in reply to content)
     * @param compatibilityBodyText The text that will appear on clients that don't support yet edition
     */
    fun editReply(
            replyToEdit: TimelineEvent,
            originalTimelineEvent: TimelineEvent,
            newBodyText: String,
            compatibilityBodyText: String = "* $newBodyText"
    ): Cancelable

    /**
     * Get the edit history of the given event.
     * The return list will contain the original event and all the editions of this event, done by the
     * same sender, sorted in the reverse order (so the original event is the latest element, and the
     * latest edition is the first element of the list)
     */
    suspend fun fetchEditHistory(eventId: String): List<Event>

    /**
     * Reply to an event in the timeline (must be in same room)
     * https://matrix.org/docs/spec/client_server/r0.4.0.html#id350
     * The replyText can be a Spannable and contains special spans (MatrixItemSpan) that will be translated
     * by the sdk into pills.
     * @param eventReplied the event referenced by the reply
     * @param replyText the reply text
     * @param autoMarkdown If true, the SDK will generate a formatted HTML message from the body text if markdown syntax is present
     * @param showInThread If true, relation will be added to the reply in order to be visible from within threads
     * @param rootThreadEventId If show in thread is true then we need the rootThreadEventId to generate the relation
     */
    fun replyToMessage(
            eventReplied: TimelineEvent,
            replyText: CharSequence,
            autoMarkdown: Boolean = false,
            showInThread: Boolean = false,
            rootThreadEventId: String? = null
    ): Cancelable?

    /**
     * Get the current EventAnnotationsSummary.
     * @param eventId the eventId to look for EventAnnotationsSummary
     * @return the EventAnnotationsSummary found
     */
    fun getEventAnnotationsSummary(eventId: String): EventAnnotationsSummary?

    /**
     * Get a LiveData of EventAnnotationsSummary for the specified eventId.
     * @param eventId the eventId to look for EventAnnotationsSummary
     * @return the LiveData of EventAnnotationsSummary
     */
    fun getEventAnnotationsSummaryLive(eventId: String): LiveData<Optional<EventAnnotationsSummary>>

    /**
     * Creates a thread reply for an existing timeline event.
     * The replyInThreadText can be a Spannable and contains special spans (MatrixItemSpan) that will be translated
     * by the sdk into pills.
     * @param rootThreadEventId the root thread eventId
     * @param replyInThreadText the reply text
     * @param msgType the message type: MessageType.MSGTYPE_TEXT (default) or MessageType.MSGTYPE_EMOTE
     * @param autoMarkdown If true, the SDK will generate a formatted HTML message from the body text if markdown syntax is present
     * @param formattedText The formatted body using MessageType#FORMAT_MATRIX_HTML
     * @param eventReplied the event referenced by the reply within a thread
     */
    fun replyInThread(
            rootThreadEventId: String,
            replyInThreadText: CharSequence,
            msgType: String = MessageType.MSGTYPE_TEXT,
            autoMarkdown: Boolean = false,
            formattedText: String? = null,
            eventReplied: TimelineEvent? = null
    ): Cancelable?
}
