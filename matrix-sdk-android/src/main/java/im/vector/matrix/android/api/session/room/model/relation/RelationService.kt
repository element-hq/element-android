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
package im.vector.matrix.android.api.session.room.model.relation

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.util.Cancelable

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
 */
interface RelationService {


    /**
     * Sends a reaction (emoji) to the targetedEvent.
     * @param reaction the reaction (preferably emoji)
     * @param targetEventId the id of the event being reacted
     */
    fun sendReaction(reaction: String, targetEventId: String): Cancelable


    /**
     * Undo a reaction (emoji) to the targetedEvent.
     * @param reaction the reaction (preferably emoji)
     * @param targetEventId the id of the event being reacted
     * @param myUserId used to know if a reaction event was made by the user
     */
    fun undoReaction(reaction: String, targetEventId: String, myUserId: String)//: Cancelable


    /**
     * Edit a text message body. Limited to "m.text" contentType
     * @param targetEventId The event to edit
     * @param newBodyText The edited body
     * @param compatibilityBodyText The text that will appear on clients that don't support yet edition
     */
    fun editTextMessage(targetEventId: String, newBodyText: String, newBodyAutoMarkdown: Boolean, compatibilityBodyText: String = "* $newBodyText"): Cancelable


    /**
     * Reply to an event in the timeline (must be in same room)
     * https://matrix.org/docs/spec/client_server/r0.4.0.html#id350
     * @param eventReplied the event referenced by the reply
     * @param replyText the reply text
     */
    fun replyToMessage(eventReplied: Event, replyText: String): Cancelable?

    fun getEventSummaryLive(eventId: String): LiveData<List<EventAnnotationsSummary>>
}