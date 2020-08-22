/*
 * Copyright 2019 New Vector Ltd
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
package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.AggregatedAnnotation
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PollSummaryContent
import org.matrix.android.sdk.api.session.room.model.ReferencesAggregatedContent
import org.matrix.android.sdk.api.session.room.model.VoteInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.ReferencesAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.create
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

enum class VerificationState {
    REQUEST,
    WAITING,
    CANCELED_BY_ME,
    CANCELED_BY_OTHER,
    DONE
}

fun VerificationState.isCanceled(): Boolean {
    return this == VerificationState.CANCELED_BY_ME || this == VerificationState.CANCELED_BY_OTHER
}

// State transition with control
private fun VerificationState?.toState(newState: VerificationState): VerificationState {
    // Cancel is always prioritary ?
    // Eg id i found that mac or keys mismatch and send a cancel and the other send a done, i have to
    // consider as canceled
    if (newState.isCanceled()) {
        return newState
    }
    // never move out of cancel
    if (this?.isCanceled() == true) {
        return this
    }
    return newState
}

internal class EventRelationsAggregationProcessor @Inject constructor(@UserId private val userId: String,
                                                                      private val cryptoService: CryptoService
) : EventInsertLiveProcessor {

    private val allowedTypes = listOf(
            EventType.MESSAGE,
            EventType.REDACTION,
            EventType.REACTION,
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_START,
            EventType.KEY_VERIFICATION_MAC,
            // TODO Add ?
            // EventType.KEY_VERIFICATION_READY,
            EventType.KEY_VERIFICATION_KEY,
            EventType.ENCRYPTED
    )

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        return allowedTypes.contains(eventType)
    }

    override suspend fun process(realm: Realm, event: Event) {
        try { // Temporary catch, should be removed
            val roomId = event.roomId
            if (roomId == null) {
                Timber.w("Event has no room id ${event.eventId}")
                return
            }
            val isLocalEcho = LocalEcho.isLocalEchoId(event.eventId ?: "")
            when (event.type) {
                EventType.REACTION             -> {
                    // we got a reaction!!
                    Timber.v("###REACTION in room $roomId , reaction eventID ${event.eventId}")
                    handleReaction(event, roomId, realm, userId, isLocalEcho)
                }
                EventType.MESSAGE              -> {
                    if (event.unsignedData?.relations?.annotations != null) {
                        Timber.v("###REACTION Agreggation in room $roomId for event ${event.eventId}")
                        handleInitialAggregatedRelations(event, roomId, event.unsignedData.relations.annotations, realm)

                        EventAnnotationsSummaryEntity.where(realm, event.eventId
                                ?: "").findFirst()?.let {
                            TimelineEventEntity.where(realm, roomId = roomId, eventId = event.eventId
                                    ?: "").findFirst()?.let { tet ->
                                tet.annotations = it
                            }
                        }
                    }

                    val content: MessageContent? = event.content.toModel()
                    if (content?.relatesTo?.type == RelationType.REPLACE) {
                        Timber.v("###REPLACE in room $roomId for event ${event.eventId}")
                        // A replace!
                        handleReplace(realm, event, content, roomId, isLocalEcho)
                    } else if (content?.relatesTo?.type == RelationType.RESPONSE) {
                        Timber.v("###RESPONSE in room $roomId for event ${event.eventId}")
                        handleResponse(realm, userId, event, content, roomId, isLocalEcho)
                    }
                }

                EventType.KEY_VERIFICATION_DONE,
                EventType.KEY_VERIFICATION_CANCEL,
                EventType.KEY_VERIFICATION_ACCEPT,
                EventType.KEY_VERIFICATION_START,
                EventType.KEY_VERIFICATION_MAC,
                EventType.KEY_VERIFICATION_READY,
                EventType.KEY_VERIFICATION_KEY -> {
                    Timber.v("## SAS REF in room $roomId for event ${event.eventId}")
                    event.content.toModel<MessageRelationContent>()?.relatesTo?.let {
                        if (it.type == RelationType.REFERENCE && it.eventId != null) {
                            handleVerification(realm, event, roomId, isLocalEcho, it.eventId, userId)
                        }
                    }
                }

                EventType.ENCRYPTED            -> {
                    // Relation type is in clear
                    val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                    if (encryptedEventContent?.relatesTo?.type == RelationType.REPLACE
                            || encryptedEventContent?.relatesTo?.type == RelationType.RESPONSE
                    ) {
                        event.getClearContent().toModel<MessageContent>()?.let {
                            if (encryptedEventContent.relatesTo.type == RelationType.REPLACE) {
                                Timber.v("###REPLACE in room $roomId for event ${event.eventId}")
                                // A replace!
                                handleReplace(realm, event, it, roomId, isLocalEcho, encryptedEventContent.relatesTo.eventId)
                            } else if (encryptedEventContent.relatesTo.type == RelationType.RESPONSE) {
                                Timber.v("###RESPONSE in room $roomId for event ${event.eventId}")
                                handleResponse(realm, userId, event, it, roomId, isLocalEcho, encryptedEventContent.relatesTo.eventId)
                            }
                        }
                    } else if (encryptedEventContent?.relatesTo?.type == RelationType.REFERENCE) {
                        when (event.getClearType()) {
                            EventType.KEY_VERIFICATION_DONE,
                            EventType.KEY_VERIFICATION_CANCEL,
                            EventType.KEY_VERIFICATION_ACCEPT,
                            EventType.KEY_VERIFICATION_START,
                            EventType.KEY_VERIFICATION_MAC,
                            EventType.KEY_VERIFICATION_READY,
                            EventType.KEY_VERIFICATION_KEY -> {
                                Timber.v("## SAS REF in room $roomId for event ${event.eventId}")
                                encryptedEventContent.relatesTo.eventId?.let {
                                    handleVerification(realm, event, roomId, isLocalEcho, it, userId)
                                }
                            }
                        }
                    }
                }
                EventType.REDACTION            -> {
                    val eventToPrune = event.redacts?.let { EventEntity.where(realm, eventId = it).findFirst() }
                            ?: return
                    when (eventToPrune.type) {
                        EventType.MESSAGE  -> {
                            Timber.d("REDACTION for message ${eventToPrune.eventId}")
//                                val unsignedData = EventMapper.map(eventToPrune).unsignedData
//                                        ?: UnsignedData(null, null)

                            // was this event a m.replace
                            val contentModel = ContentMapper.map(eventToPrune.content)?.toModel<MessageContent>()
                            if (RelationType.REPLACE == contentModel?.relatesTo?.type && contentModel.relatesTo?.eventId != null) {
                                handleRedactionOfReplace(eventToPrune, contentModel.relatesTo!!.eventId!!, realm)
                            }
                        }
                        EventType.REACTION -> {
                            handleReactionRedact(eventToPrune, realm, userId)
                        }
                    }
                }
                else                           -> Timber.v("UnHandled event ${event.eventId}")
            }
        } catch (t: Throwable) {
            Timber.e(t, "## Should not happen ")
        }
    }

    // OPT OUT serer aggregation until API mature enough
    private val SHOULD_HANDLE_SERVER_AGREGGATION = false

    private fun handleReplace(realm: Realm, event: Event, content: MessageContent, roomId: String, isLocalEcho: Boolean, relatedEventId: String? = null) {
        val eventId = event.eventId ?: return
        val targetEventId = relatedEventId ?: content.relatesTo?.eventId ?: return
        val newContent = content.newContent ?: return
        // ok, this is a replace
        val existing = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId, targetEventId)

        // we have it
        val existingSummary = existing.editSummary
        if (existingSummary == null) {
            Timber.v("###REPLACE new edit summary for $targetEventId, creating one (localEcho:$isLocalEcho)")
            // create the edit summary
            val editSummary = realm.createObject(EditAggregatedSummaryEntity::class.java)
            editSummary.aggregatedContent = ContentMapper.map(newContent)
            if (isLocalEcho) {
                editSummary.lastEditTs = 0
                editSummary.sourceLocalEchoEvents.add(eventId)
            } else {
                editSummary.lastEditTs = event.originServerTs ?: 0
                editSummary.sourceEvents.add(eventId)
            }

            existing.editSummary = editSummary
        } else {
            if (existingSummary.sourceEvents.contains(eventId)) {
                // ignore this event, we already know it (??)
                Timber.v("###REPLACE ignoring event for summary, it's known $eventId")
                return
            }
            val txId = event.unsignedData?.transactionId
            // is it a remote echo?
            if (!isLocalEcho && existingSummary.sourceLocalEchoEvents.contains(txId)) {
                // ok it has already been managed
                Timber.v("###REPLACE Receiving remote echo of edit (edit already done)")
                existingSummary.sourceLocalEchoEvents.remove(txId)
                existingSummary.sourceEvents.add(event.eventId)
            } else if (
                    isLocalEcho // do not rely on ts for local echo, take it
                    || event.originServerTs ?: 0 >= existingSummary.lastEditTs
            ) {
                Timber.v("###REPLACE Computing aggregated edit summary (isLocalEcho:$isLocalEcho)")
                if (!isLocalEcho) {
                    // Do not take local echo originServerTs here, could mess up ordering (keep old ts)
                    existingSummary.lastEditTs = event.originServerTs ?: System.currentTimeMillis()
                }
                existingSummary.aggregatedContent = ContentMapper.map(newContent)
                if (isLocalEcho) {
                    existingSummary.sourceLocalEchoEvents.add(eventId)
                } else {
                    existingSummary.sourceEvents.add(eventId)
                }
            } else {
                // ignore this event for the summary (back paginate)
                if (!isLocalEcho) {
                    existingSummary.sourceEvents.add(eventId)
                }
                Timber.v("###REPLACE ignoring event for summary, it's to old $eventId")
            }
        }
    }

    private fun handleResponse(realm: Realm,
                               userId: String,
                               event: Event,
                               content: MessageContent,
                               roomId: String,
                               isLocalEcho: Boolean,
                               relatedEventId: String? = null) {
        val eventId = event.eventId ?: return
        val senderId = event.senderId ?: return
        val targetEventId = relatedEventId ?: content.relatesTo?.eventId ?: return
        val eventTimestamp = event.originServerTs ?: return

        // ok, this is a poll response
        var existing = EventAnnotationsSummaryEntity.where(realm, targetEventId).findFirst()
        if (existing == null) {
            Timber.v("## POLL creating new relation summary for $targetEventId")
            existing = EventAnnotationsSummaryEntity.create(realm, roomId, targetEventId)
        }

        // we have it
        val existingPollSummary = existing.pollResponseSummary
                ?: realm.createObject(PollResponseAggregatedSummaryEntity::class.java).also {
                    existing.pollResponseSummary = it
                }

        val closedTime = existingPollSummary?.closedTime
        if (closedTime != null && eventTimestamp > closedTime) {
            Timber.v("## POLL is closed ignore event poll:$targetEventId, event :${event.eventId}")
            return
        }

        val sumModel = ContentMapper.map(existingPollSummary?.aggregatedContent).toModel<PollSummaryContent>() ?: PollSummaryContent()

        if (existingPollSummary!!.sourceEvents.contains(eventId)) {
            // ignore this event, we already know it (??)
            Timber.v("## POLL  ignoring event for summary, it's known eventId:$eventId")
            return
        }
        val txId = event.unsignedData?.transactionId
        // is it a remote echo?
        if (!isLocalEcho && existingPollSummary.sourceLocalEchoEvents.contains(txId)) {
            // ok it has already been managed
            Timber.v("## POLL  Receiving remote echo of response eventId:$eventId")
            existingPollSummary.sourceLocalEchoEvents.remove(txId)
            existingPollSummary.sourceEvents.add(event.eventId)
            return
        }

        val responseContent = event.content.toModel<MessagePollResponseContent>() ?: return Unit.also {
            Timber.d("## POLL  Receiving malformed response eventId:$eventId content: ${event.content}")
        }

        val optionIndex = responseContent.relatesTo?.option ?: return Unit.also {
            Timber.d("## POLL Ignoring malformed response no option eventId:$eventId content: ${event.content}")
        }

        val votes = sumModel.votes?.toMutableList() ?: ArrayList()
        val existingVoteIndex = votes.indexOfFirst { it.userId == senderId }
        if (existingVoteIndex != -1) {
            // Is the vote newer?
            val existingVote = votes[existingVoteIndex]
            if (existingVote.voteTimestamp < eventTimestamp) {
                // Take the new one
                votes[existingVoteIndex] = VoteInfo(senderId, optionIndex, eventTimestamp)
                if (userId == senderId) {
                    sumModel.myVote = optionIndex
                }
                Timber.v("## POLL adding vote $optionIndex for user $senderId in poll :$relatedEventId ")
            } else {
                Timber.v("## POLL Ignoring vote (older than known one)  eventId:$eventId ")
            }
        } else {
            votes.add(VoteInfo(senderId, optionIndex, eventTimestamp))
            if (userId == senderId) {
                sumModel.myVote = optionIndex
            }
            Timber.v("## POLL adding vote $optionIndex for user $senderId in poll :$relatedEventId ")
        }
        sumModel.votes = votes
        if (isLocalEcho) {
            existingPollSummary.sourceLocalEchoEvents.add(eventId)
        } else {
            existingPollSummary.sourceEvents.add(eventId)
        }

        existingPollSummary.aggregatedContent = ContentMapper.map(sumModel.toContent())
    }

    private fun handleInitialAggregatedRelations(event: Event, roomId: String, aggregation: AggregatedAnnotation, realm: Realm) {
        if (SHOULD_HANDLE_SERVER_AGREGGATION) {
            aggregation.chunk?.forEach {
                if (it.type == EventType.REACTION) {
                    val eventId = event.eventId ?: ""
                    val existing = EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
                    if (existing == null) {
                        val eventSummary = EventAnnotationsSummaryEntity.create(realm, roomId, eventId)
                        val sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                        sum.key = it.key
                        sum.firstTimestamp = event.originServerTs
                                ?: 0 // TODO how to maintain order?
                        sum.count = it.count
                        eventSummary.reactionsSummary.add(sum)
                    } else {
                        // TODO how to handle that
                    }
                }
            }
        }
    }

    private fun handleReaction(event: Event, roomId: String, realm: Realm, userId: String, isLocalEcho: Boolean) {
        val content = event.content.toModel<ReactionContent>()
        if (content == null) {
            Timber.e("Malformed reaction content ${event.content}")
            return
        }
        // rel_type must be m.annotation
        if (RelationType.ANNOTATION == content.relatesTo?.type) {
            val reaction = content.relatesTo.key
            val relatedEventID = content.relatesTo.eventId
            val reactionEventId = event.eventId
            Timber.v("Reaction $reactionEventId relates to $relatedEventID")
            val eventSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId, relatedEventID)

            var sum = eventSummary.reactionsSummary.find { it.key == reaction }
            val txId = event.unsignedData?.transactionId
            if (isLocalEcho && txId.isNullOrBlank()) {
                Timber.w("Received a local echo with no transaction ID")
            }
            if (sum == null) {
                sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                sum.key = reaction
                sum.firstTimestamp = event.originServerTs ?: 0
                if (isLocalEcho) {
                    Timber.v("Adding local echo reaction $reaction")
                    sum.sourceLocalEcho.add(txId)
                    sum.count = 1
                } else {
                    Timber.v("Adding synced reaction $reaction")
                    sum.count = 1
                    sum.sourceEvents.add(reactionEventId)
                }
                sum.addedByMe = sum.addedByMe || (userId == event.senderId)
                eventSummary.reactionsSummary.add(sum)
            } else {
                // is this a known event (is possible? pagination?)
                if (!sum.sourceEvents.contains(reactionEventId)) {
                    // check if it's not the sync of a local echo
                    if (!isLocalEcho && sum.sourceLocalEcho.contains(txId)) {
                        // ok it has already been counted, just sync the list, do not touch count
                        Timber.v("Ignoring synced of local echo for reaction $reaction")
                        sum.sourceLocalEcho.remove(txId)
                        sum.sourceEvents.add(reactionEventId)
                    } else {
                        sum.count += 1
                        if (isLocalEcho) {
                            Timber.v("Adding local echo reaction $reaction")
                            sum.sourceLocalEcho.add(txId)
                        } else {
                            Timber.v("Adding synced reaction $reaction")
                            sum.sourceEvents.add(reactionEventId)
                        }

                        sum.addedByMe = sum.addedByMe || (userId == event.senderId)
                    }
                }
            }
        } else {
            Timber.e("Unknown relation type ${content.relatesTo?.type} for event ${event.eventId}")
        }
    }

    /**
     * Called when an event is deleted
     */
    private fun handleRedactionOfReplace(redacted: EventEntity, relatedEventId: String, realm: Realm) {
        Timber.d("Handle redaction of m.replace")
        val eventSummary = EventAnnotationsSummaryEntity.where(realm, relatedEventId).findFirst()
        if (eventSummary == null) {
            Timber.w("Redaction of a replace targeting an unknown event $relatedEventId")
            return
        }
        val sourceEvents = eventSummary.editSummary?.sourceEvents
        val sourceToDiscard = sourceEvents?.indexOf(redacted.eventId)
        if (sourceToDiscard == null) {
            Timber.w("Redaction of a replace that was not known in aggregation $sourceToDiscard")
            return
        }
        // Need to remove this event from the redaction list and compute new aggregation state
        sourceEvents.removeAt(sourceToDiscard)
        val previousEdit = sourceEvents.mapNotNull { EventEntity.where(realm, it).findFirst() }.sortedBy { it.originServerTs }.lastOrNull()
        if (previousEdit == null) {
            // revert to original
            eventSummary.editSummary?.deleteFromRealm()
        } else {
            // I have the last event
            ContentMapper.map(previousEdit.content)?.toModel<MessageContent>()?.newContent?.let { newContent ->
                eventSummary.editSummary?.lastEditTs = previousEdit.originServerTs
                        ?: System.currentTimeMillis()
                eventSummary.editSummary?.aggregatedContent = ContentMapper.map(newContent)
            } ?: run {
                Timber.e("Failed to udate edited summary")
                // TODO how to reccover that
            }
        }
    }

    fun handleReactionRedact(eventToPrune: EventEntity, realm: Realm, userId: String) {
        Timber.v("REDACTION of reaction ${eventToPrune.eventId}")
        // delete a reaction, need to update the annotation summary if any
        val reactionContent: ReactionContent = EventMapper.map(eventToPrune).content.toModel()
                ?: return
        val eventThatWasReacted = reactionContent.relatesTo?.eventId ?: return

        val reactionKey = reactionContent.relatesTo.key
        Timber.v("REMOVE reaction for key $reactionKey")
        val summary = EventAnnotationsSummaryEntity.where(realm, eventThatWasReacted).findFirst()
        if (summary != null) {
            summary.reactionsSummary.where()
                    .equalTo(ReactionAggregatedSummaryEntityFields.KEY, reactionKey)
                    .findFirst()?.let { aggregation ->
                        Timber.v("Find summary for key with  ${aggregation.sourceEvents.size} known reactions (count:${aggregation.count})")
                        Timber.v("Known reactions  ${aggregation.sourceEvents.joinToString(",")}")
                        if (aggregation.sourceEvents.contains(eventToPrune.eventId)) {
                            Timber.v("REMOVE reaction for key $reactionKey")
                            aggregation.sourceEvents.remove(eventToPrune.eventId)
                            Timber.v("Known reactions after  ${aggregation.sourceEvents.joinToString(",")}")
                            aggregation.count = aggregation.count - 1
                            if (eventToPrune.sender == userId) {
                                // Was it a redact on my reaction?
                                aggregation.addedByMe = false
                            }
                            if (aggregation.count == 0) {
                                // delete!
                                aggregation.deleteFromRealm()
                            }
                        } else {
                            Timber.e("## Cannot remove summary from count, corresponding reaction ${eventToPrune.eventId} is not known")
                        }
                    }
        } else {
            Timber.e("## Cannot find summary for key $reactionKey")
        }
    }

    private fun handleVerification(realm: Realm, event: Event, roomId: String, isLocalEcho: Boolean, relatedEventId: String, userId: String) {
        val eventSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId, relatedEventId)

        val verifSummary = eventSummary.referencesSummaryEntity
                ?: ReferencesAggregatedSummaryEntity.create(realm, relatedEventId).also {
                    eventSummary.referencesSummaryEntity = it
                }

        val txId = event.unsignedData?.transactionId

        if (!isLocalEcho && verifSummary.sourceLocalEcho.contains(txId)) {
            // ok it has already been handled
        } else {
            ContentMapper.map(verifSummary.content)?.toModel<ReferencesAggregatedContent>()
            var data = ContentMapper.map(verifSummary.content)?.toModel<ReferencesAggregatedContent>()
                    ?: ReferencesAggregatedContent(VerificationState.REQUEST)
            // TODO ignore invalid messages? e.g a START after a CANCEL?
            // i.e. never change state if already canceled/done
            val currentState = data.verificationState
            val newState = when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START,
                EventType.KEY_VERIFICATION_ACCEPT,
                EventType.KEY_VERIFICATION_READY,
                EventType.KEY_VERIFICATION_KEY,
                EventType.KEY_VERIFICATION_MAC    -> currentState.toState(VerificationState.WAITING)
                EventType.KEY_VERIFICATION_CANCEL -> currentState.toState(if (event.senderId == userId) {
                    VerificationState.CANCELED_BY_ME
                } else {
                    VerificationState.CANCELED_BY_OTHER
                })
                EventType.KEY_VERIFICATION_DONE   -> currentState.toState(VerificationState.DONE)
                else                              -> VerificationState.REQUEST
            }

            data = data.copy(verificationState = newState)
            verifSummary.content = ContentMapper.map(data.toContent())
        }

        if (isLocalEcho) {
            verifSummary.sourceLocalEcho.add(event.eventId)
        } else {
            verifSummary.sourceLocalEcho.remove(txId)
            verifSummary.sourceEvents.add(event.eventId)
        }
    }
}
