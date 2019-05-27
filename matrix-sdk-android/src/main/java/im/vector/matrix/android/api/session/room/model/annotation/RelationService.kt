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
package im.vector.matrix.android.api.session.room.model.annotation

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Cancelable

//TODO rename in relationService?
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
     * Update a quick reaction (toggle).
     * If you have reacted with agree and then you click on disagree, this call will delete(redact)
     * the disagree and add the agree
     * If you click on a reaction that you already reacted with, it will undo it
     * @param reaction the reaction (preferably emoji)
     * @param oppositeReaction the opposite reaction(preferably emoji)
     * @param targetEventId the id of the event being reacted
     * @param myUserId used to know if a reaction event was made by the user
     */
    fun updateQuickReaction(reaction: String, oppositeReaction: String, targetEventId: String, myUserId: String)


    /**
     * Edit a text message body. Limited to "m.text" contentType
     * @param targetEventId The event to edit
     * @param newBodyText The edited body
     * @param compatibilityBodyText The text that will appear on clients that don't support yet edition
     */
    fun editTextMessage(targetEventId: String, newBodyText: String, compatibilityBodyText: String = "* $newBodyText"): Cancelable


    fun replyToMessage(eventReplied: Event, replyText: String) : Cancelable?

}