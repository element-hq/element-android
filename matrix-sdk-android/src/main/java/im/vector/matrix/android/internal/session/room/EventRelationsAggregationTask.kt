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
package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.PollSummaryContent
import im.vector.matrix.android.api.session.room.model.ReferencesAggregatedContent
import im.vector.matrix.android.api.session.room.model.VoteInfo
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessagePollResponseContent
import im.vector.matrix.android.api.session.room.model.message.MessageRelationContent
import im.vector.matrix.android.api.session.room.model.relation.ReactionContent
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.EventMapper
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.ReactionAggregatedSummaryEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.*
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal interface EventRelationsAggregationTask : Task<EventRelationsAggregationTask.Params, Unit> {

    data class Params(
            val eventInsertNotifications: List<EventInsertNotification>
    )
}

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

/**
 * Called by EventRelationAggregationUpdater, when new events that can affect relations are inserted in base.
 */
internal class DefaultEventRelationsAggregationTask @Inject constructor(
        private val sessionDatabase: SessionDatabase,
        @UserId private val userId: String,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoService: CryptoService) : EventRelationsAggregationTask {

    // OPT OUT serer aggregation until API mature enough
    private val SHOULD_HANDLE_SERVER_AGREGGATION = false

    override suspend fun execute(params: EventRelationsAggregationTask.Params) {
        val eventInsertNotifications = params.eventInsertNotifications
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            Timber.v(">>> DefaultEventRelationsAggregationTask[${params.hashCode()}] called with ${eventInsertNotifications.size} events")
            update(eventInsertNotifications, userId)
            Timber.v("<<< DefaultEventRelationsAggregationTask[${params.hashCode()}] finished")
        }
    }

    private fun update(eventInsertNotifications: List<EventInsertNotification>, userId: String) {
        eventInsertNotifications.forEach { eventInsertNotification ->
            try { // Temporary catch, should be removed
                val roomId = eventInsertNotification.room_id
                val eventId = eventInsertNotification.event_id
                val isLocalEcho = LocalEcho.isLocalEchoId(eventId)
                val event = sessionDatabase.eventQueries.select(eventId).executeAsOneOrNull()?.asDomain()
                        ?: return@forEach
                when (eventInsertNotification.type) {
                    EventType.REACTION -> {
                        // we got a reaction!!
                        Timber.v("###REACTION in room $roomId , reaction eventID ${eventId}")
                        handleReaction(event, roomId, userId, isLocalEcho)
                    }

                    EventType.MESSAGE -> {
                        if (event.unsignedData?.relations?.annotations != null) {
                            Timber.v("###REACTION Agreggation in room $roomId for event ${event.eventId}")
                            //handleInitialAggregatedRelations(event, roomId, event.unsignedData.relations.annotations)
                        }

                        val content: MessageContent? = event.content.toModel()
                        if (content?.relatesTo?.type == RelationType.REPLACE) {
                            Timber.v("###REPLACE in room $roomId for event ${event.eventId}")
                            // A replace!
                            handleReplace(event, content, roomId, isLocalEcho)
                        } else if (content?.relatesTo?.type == RelationType.RESPONSE) {
                            Timber.v("###RESPONSE in room $roomId for event ${event.eventId}")
                            handleResponse(userId, event, content, roomId, isLocalEcho)
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
                                handleVerification(event, roomId, isLocalEcho, it.eventId, userId)
                            }
                        }
                    }

                    EventType.ENCRYPTED -> {
                        // Relation type is in clear
                        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                        if (encryptedEventContent?.relatesTo?.type == RelationType.REPLACE
                                || encryptedEventContent?.relatesTo?.type == RelationType.RESPONSE
                        ) {
                            // we need to decrypt if needed
                            decryptIfNeeded(event)
                            event.getClearContent().toModel<MessageContent>()?.let {
                                if (encryptedEventContent.relatesTo.type == RelationType.REPLACE) {
                                    Timber.v("###REPLACE in room $roomId for event ${event.eventId}")
                                    // A replace!
                                    handleReplace(event, it, roomId, isLocalEcho, encryptedEventContent.relatesTo.eventId)
                                } else if (encryptedEventContent.relatesTo.type == RelationType.RESPONSE) {
                                    Timber.v("###RESPONSE in room $roomId for event ${event.eventId}")
                                    //handleResponse( realm, userId, event, it, roomId, isLocalEcho, encryptedEventContent.relatesTo.eventId)
                                }
                            }
                        } else if (encryptedEventContent?.relatesTo?.type == RelationType.REFERENCE) {
                            decryptIfNeeded(event)
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
                                        handleVerification(event, roomId, isLocalEcho, it, userId)
                                    }
                                }
                            }
                        }
                    }
                    EventType.REDACTION -> {
                        val eventToPrune = event.redacts?.let { sessionDatabase.eventQueries.select(event.redacts).executeAsOneOrNull() }
                                ?: return@forEach
                        when (eventToPrune.type) {
                            EventType.MESSAGE -> {
                                Timber.d("REDACTION for message ${eventToPrune.event_id}")
//                                val unsignedData = EventMapper.map(eventToPrune).unsignedData
//                                        ?: UnsignedData(null, null)

                                // was this event a m.replace
                                val contentModel = ContentMapper.map(eventToPrune.content)?.toModel<MessageContent>()
                                if (RelationType.REPLACE == contentModel?.relatesTo?.type && contentModel.relatesTo?.eventId != null) {
                                    handleRedactionOfReplace(eventToPrune, contentModel.relatesTo!!.eventId!!, roomId)
                                }
                            }
                            EventType.REACTION -> {
                                handleReactionRedact(eventToPrune, roomId)
                            }
                        }
                    }
                    else -> Timber.v("UnHandled event ${eventInsertNotification.event_id}")
                }
            } catch (t: Throwable) {
                Timber.e(t, "## Should not happen ")
            }
        }
    }

    private fun decryptIfNeeded(event: Event) {
        if (event.mxDecryptionResult == null) {
            try {
                val result = cryptoService.decryptEvent(event, event.roomId ?: "")
                event.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderCurve25519Key,
                        keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                )
            } catch (e: MXCryptoError) {
                Timber.v("Failed to decrypt e2e replace")
                // TODO -> we should keep track of this and retry, or aggregation will be broken
            }
        }
    }

    private fun handleReplace(event: Event, content: MessageContent, roomId: String, isLocalEcho: Boolean, relatedEventId: String? = null) {
        val eventId = event.eventId ?: return
        val targetEventId = relatedEventId ?: content.relatesTo?.eventId ?: return
        val newContent = content.newContent ?: return
        // we have it
        val existingSummary = sessionDatabase.eventAnnotationsQueries.selectEditForEvent(targetEventId).executeAsOneOrNull()
        if (existingSummary == null) {
            Timber.v("###REPLACE new edit summary for $targetEventId, creating one (localEcho:$isLocalEcho)")
            // create the edit summary
            val lastEditTs: Long
            val sourceLocalEchoEvents: List<String>
            val sourceEvents: List<String>
            if (isLocalEcho) {
                lastEditTs = 0
                sourceLocalEchoEvents = listOf(eventId)
                sourceEvents = emptyList()
            } else {
                lastEditTs = event.originServerTs ?: 0
                sourceLocalEchoEvents = emptyList()
                sourceEvents = listOf(eventId)
            }
            val newEditSummary = EditAggregatedSummary.Impl(
                    event_id = targetEventId,
                    room_id = roomId,
                    aggregated_content = ContentMapper.map(newContent),
                    last_edit_ts = lastEditTs,
                    source_local_echo_ids = sourceLocalEchoEvents,
                    source_event_ids = sourceEvents
            )
            sessionDatabase.eventAnnotationsQueries.insertNewEdit(newEditSummary)
        } else {
            val sourceEvents = existingSummary.source_event_ids.toMutableList()
            if (sourceEvents.contains(eventId)) {
                // ignore this event, we already know it (??)
                Timber.v("###REPLACE ignoring event for summary, it's known $eventId")
                return
            }
            val txId = event.unsignedData?.transactionId
            // is it a remote echo?
            val sourceLocalEchoEvents = existingSummary.source_local_echo_ids.toMutableList()
            if (!isLocalEcho && sourceLocalEchoEvents.contains(txId)) {
                // ok it has already been managed
                Timber.v("###REPLACE Receiving remote echo of edit (edit already done)")
                sourceLocalEchoEvents.remove(txId)
                sourceEvents.add(event.eventId)

            } else if (
                    isLocalEcho // do not rely on ts for local echo, take it
                    || event.originServerTs ?: 0 >= existingSummary.last_edit_ts
            ) {
                Timber.v("###REPLACE Computing aggregated edit summary (isLocalEcho:$isLocalEcho)")
                if (!isLocalEcho) {
                    // Do not take local echo originServerTs here, could mess up ordering (keep old ts)
                    val newLastEditTs = event.originServerTs ?: System.currentTimeMillis()
                    sessionDatabase.eventAnnotationsQueries.updateEditLastTs(newLastEditTs, targetEventId)
                }
                val newAggregatedContent = ContentMapper.map(newContent)
                sessionDatabase.eventAnnotationsQueries.updateEditContent(newAggregatedContent, targetEventId)
                if (isLocalEcho) {
                    sourceLocalEchoEvents.add(eventId)
                } else {
                    sourceEvents.add(eventId)
                }
            } else {
                // ignore this event for the summary (back paginate)
                if (!isLocalEcho) {
                    sourceEvents.add(eventId)
                }
                Timber.v("###REPLACE ignoring event for summary, it's to old $eventId")
            }
            sessionDatabase.eventAnnotationsQueries.updateEditSources(
                    sourceEventIds = sourceEvents,
                    sourceLocalEchoIds = sourceLocalEchoEvents,
                    eventId = targetEventId
            )
        }
    }

    private fun handleResponse(userId: String,
                               event: Event,
                               content: MessageContent,
                               roomId: String,
                               isLocalEcho: Boolean,
                               relatedEventId: String? = null) {
        val eventId = event.eventId ?: return
        val senderId = event.senderId ?: return
        val targetEventId = relatedEventId ?: content.relatesTo?.eventId ?: return
        val eventTimestamp = event.originServerTs ?: return

        val existingPollSummary = sessionDatabase.eventAnnotationsQueries.selectPollForEvent(targetEventId).executeAsOneOrNull()
        val closedTime = existingPollSummary?.closed_time
        if (closedTime != null && eventTimestamp > closedTime) {
            Timber.v("## POLL is closed ignore event poll:$targetEventId, event :${event.eventId}")
            return
        }
        val sumModel = ContentMapper.map(existingPollSummary?.content).toModel<PollSummaryContent>()
                ?: PollSummaryContent()

        val sourceEvents = existingPollSummary?.source_event_ids?.toMutableList() ?: ArrayList()
        if (sourceEvents.contains(eventId)) {
            // ignore this event, we already know it (??)
            Timber.v("## POLL  ignoring event for summary, it's known eventId:$eventId")
            return
        }
        val txId = event.unsignedData?.transactionId
        // is it a remote echo?
        val sourceLocalEchoEvents = existingPollSummary?.source_local_echo_ids?.toMutableList()
                ?: ArrayList()
        if (!isLocalEcho && sourceLocalEchoEvents.contains(txId)) {
            // ok it has already been managed
            Timber.v("## POLL  Receiving remote echo of response eventId:$eventId")
            sourceLocalEchoEvents.remove(txId)
            sourceEvents.add(event.eventId)
            sessionDatabase.eventAnnotationsQueries.updatePollSources(sourceEvents, sourceLocalEchoEvents, targetEventId)
            return
        }

        val responseContent = event.content.toModel<MessagePollResponseContent>()
                ?: return Unit.also {
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
            sourceLocalEchoEvents.add(eventId)
        } else {
            sourceEvents.add(eventId)
        }
        val newContent = ContentMapper.map(sumModel.toContent())
        sessionDatabase.eventAnnotationsQueries.updatePollSources(sourceEvents, sourceLocalEchoEvents, targetEventId)
        sessionDatabase.eventAnnotationsQueries.updatePollContent(newContent, targetEventId)
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

    private fun handleReaction(event: Event, roomId: String, userId: String, isLocalEcho: Boolean) {
        val content = event.content.toModel<ReactionContent>()
        if (content == null) {
            Timber.e("Malformed reaction content ${event.content}")
            return
        }
        // rel_type must be m.annotation
        if (RelationType.ANNOTATION == content.relatesTo?.type) {
            val reaction = content.relatesTo.key
            val relatedEventID = content.relatesTo.eventId
            val reactionEventId = event.eventId ?: return
            Timber.v("Reaction $reactionEventId relates to $relatedEventID")
            val reactionSummary = sessionDatabase.eventAnnotationsQueries.selectReaction(relatedEventID, reaction).executeAsOneOrNull()
            val txId = event.unsignedData?.transactionId
            if (isLocalEcho && txId.isNullOrBlank()) {
                Timber.w("Received a local echo with no transaction ID")
                return
            }
            if (reactionSummary == null) {
                Timber.v("$reaction is a new reaction")
                val (sourceEventIds, sourceLocalEchoIds) = if (isLocalEcho && txId != null) {
                    Timber.v("Adding local echo reaction $reaction")
                    Pair(emptyList(), listOf(txId))
                } else {
                    Timber.v("Adding synced reaction $reaction")
                    Pair(listOf(reactionEventId), emptyList<String>())
                }
                val newReactionSummary = ReactionAggregatedSummary.Impl(
                        event_id = relatedEventID,
                        room_id = roomId,
                        key = reaction,
                        count = 1,
                        added_by_me = userId == event.senderId,
                        first_timestamp = event.originServerTs ?: 0,
                        source_event_ids = sourceEventIds,
                        source_local_echo_ids = sourceLocalEchoIds
                )
                sessionDatabase.eventAnnotationsQueries.insertNewReaction(newReactionSummary)
            } else {
                Timber.v("$reaction is an already known reaction")
                // is this a known event (is possible? pagination?)
                val sourceEvents = reactionSummary.source_event_ids.toMutableList()
                if (!sourceEvents.contains(reactionEventId)) {
                    // check if it's not the sync of a local echo
                    val sourceLocalEcho = reactionSummary.source_local_echo_ids.toMutableList()
                    if (!isLocalEcho && sourceLocalEcho.contains(txId)) {
                        // ok it has already been counted, just sync the list, do not touch count
                        Timber.v("Ignoring synced of local echo for reaction $reaction")
                        sourceLocalEcho.remove(txId)
                        sourceEvents.add(reactionEventId)
                        sessionDatabase.eventAnnotationsQueries.updateLocalReaction(
                                sourceEventIds = sourceEvents,
                                sourceLocalEchoIds = sourceLocalEcho,
                                eventId = relatedEventID,
                                key = reaction
                        )
                    } else {
                        val newCount = reactionSummary.count + 1
                        val newAddedByMe = reactionSummary.added_by_me || (userId == event.senderId)
                        if (isLocalEcho && txId != null) {
                            Timber.v("Adding local echo reaction $reaction")
                            sourceLocalEcho.add(txId)
                        } else {
                            Timber.v("Adding synced reaction $reaction")
                            sourceEvents.add(reactionEventId)
                        }
                        sessionDatabase.eventAnnotationsQueries.updateReaction(
                                count = newCount,
                                addedByMe = newAddedByMe,
                                sourceEventIds = sourceEvents,
                                sourceLocalEchoIds = sourceLocalEcho,
                                eventId = relatedEventID,
                                key = reaction
                        )
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
    private fun handleRedactionOfReplace(redacted: EventEntity, relatedEventId: String, roomId: String) {
        Timber.d("Handle redaction of m.replace")
        val editSummary = sessionDatabase.eventAnnotationsQueries.selectEditForEvent(relatedEventId).executeAsOneOrNull()
        if (editSummary == null) {
            Timber.w("Redaction of a replace targeting an unknown event $relatedEventId")
            return
        }
        val sourceEvents = editSummary.source_event_ids.toMutableList()
        val sourceToDiscard = sourceEvents.indexOf(redacted.event_id)
        if (sourceToDiscard == -1) {
            Timber.w("Redaction of a replace that was not known in aggregation $sourceToDiscard")
            return
        }
        // Need to remove this event from the redaction list and compute new aggregation state
        sourceEvents.removeAt(sourceToDiscard)
        val previousEdit = sourceEvents.mapNotNull { sessionDatabase.eventQueries.select(it).executeAsOneOrNull() }.sortedBy { it.origin_server_ts }.lastOrNull()
        if (previousEdit == null) {
            // revert to original
            sessionDatabase.eventAnnotationsQueries.deleteEdit(relatedEventId, roomId)
        } else {
            // I have the last event
            ContentMapper.map(previousEdit.content)?.toModel<MessageContent>()?.newContent?.let { newContent ->
                val newLastEditTs = previousEdit.origin_server_ts
                        ?: System.currentTimeMillis()
                val newAggregatedContent = ContentMapper.map(newContent)
                sessionDatabase.eventAnnotationsQueries.updateEditSources(sourceEvents, editSummary.source_local_echo_ids, relatedEventId)
                sessionDatabase.eventAnnotationsQueries.updateEditLastTs(newLastEditTs, relatedEventId)
                sessionDatabase.eventAnnotationsQueries.updateEditContent(newAggregatedContent, relatedEventId)
            } ?: run {
                Timber.e("Failed to udate edited summary")
                // TODO how to reccover that
            }
        }
    }

    fun handleReactionRedact(eventToPrune: EventEntity, roomId: String) {
        Timber.v("REDACTION of reaction ${eventToPrune.event_id}")
        // delete a reaction, need to update the annotation summary if any
        val reactionContent: ReactionContent = EventMapper.map(eventToPrune).content.toModel()
                ?: return
        val eventThatWasReacted = reactionContent.relatesTo?.eventId ?: return

        val reactionKey = reactionContent.relatesTo.key
        Timber.v("REMOVE reaction for key $reactionKey")
        val reactionSummary = sessionDatabase.eventAnnotationsQueries.selectReaction(eventThatWasReacted, reactionKey).executeAsOneOrNull()
        if (reactionSummary != null) {
            val sourceEvents = reactionSummary.source_event_ids.toMutableList()
            Timber.v("Find summary for key with  ${sourceEvents.size} known reactions (count:${reactionSummary.count})")
            if (sourceEvents.contains(eventToPrune.event_id)) {
                Timber.v("REMOVE reaction for key $reactionKey")
                sourceEvents.remove(eventToPrune.event_id)
                val newCount = reactionSummary.count - 1
                val addedByMe = if (eventToPrune.sender_id == userId) {
                    // Was it a redact on my reaction?
                    false
                } else {
                    reactionSummary.added_by_me
                }
                if (newCount == 0L) {
                    sessionDatabase.eventAnnotationsQueries.deleteReaction(eventThatWasReacted, roomId, reactionKey)
                } else {
                    sessionDatabase.eventAnnotationsQueries.updateReaction(
                            count = newCount,
                            addedByMe = addedByMe,
                            sourceEventIds = sourceEvents,
                            sourceLocalEchoIds = reactionSummary.source_local_echo_ids,
                            eventId = eventThatWasReacted,
                            key = reactionKey
                    )
                }
            } else {
                Timber.e("## Cannot remove summary from count, corresponding reaction ${eventToPrune.event_id} is not known")
            }
        } else {
            Timber.e("## Cannot find summary for key $reactionKey")
        }
    }

    private fun handleVerification(event: Event, roomId: String, isLocalEcho: Boolean, relatedEventId: String, userId: String) {
        val eventId = event.eventId ?: return
        val verifSummary = sessionDatabase.eventAnnotationsQueries.selectReferenceForEvent(relatedEventId).executeAsOneOrNull()
        if (verifSummary == null) {
            val state = VerificationState.REQUEST.computeNewVerificationState(event)
            val data = ReferencesAggregatedContent(state)
            val sourceLocalEchoEvents: List<String>
            val sourceEvents: List<String>
            if (isLocalEcho) {
                sourceLocalEchoEvents = listOf(eventId)
                sourceEvents = emptyList()
            } else {
                sourceLocalEchoEvents = emptyList()
                sourceEvents = listOf(eventId)
            }
            val newVerifSummary = ReferencesAggregatedSummary.Impl(
                    event_id = relatedEventId,
                    room_id = roomId,
                    content = ContentMapper.map(data.toContent()),
                    source_local_echo_ids = sourceLocalEchoEvents,
                    source_event_ids = sourceEvents
            )
            sessionDatabase.eventAnnotationsQueries.insertNewReference(newVerifSummary)

        } else {
            val txId = event.unsignedData?.transactionId
            val sourceEvents = verifSummary.source_event_ids.toMutableList()
            val sourceLocalEcho = verifSummary.source_local_echo_ids.toMutableList()
            if (!isLocalEcho && sourceLocalEcho.contains(txId)) {
                // ok it has already been handled
            } else {
                var data = ContentMapper.map(verifSummary.content)?.toModel<ReferencesAggregatedContent>()
                        ?: ReferencesAggregatedContent(VerificationState.REQUEST)
                // TODO ignore invalid messages? e.g a START after a CANCEL?
                // i.e. never change state if already canceled/done
                val currentState = data.verificationState
                val newState = currentState.computeNewVerificationState(event)
                data = data.copy(verificationState = newState)
                val newContent = ContentMapper.map(data.toContent())
                sessionDatabase.eventAnnotationsQueries.updateReferenceContent(newContent, relatedEventId)
            }
            if (isLocalEcho) {
                sourceLocalEcho.add(eventId)
            } else {
                sourceLocalEcho.remove(txId)
                sourceEvents.add(event.eventId)
            }
            sessionDatabase.eventAnnotationsQueries.updateReferenceSources(
                    sourceEventIds = sourceEvents,
                    sourceLocalEchoIds = sourceLocalEcho,
                    eventId = relatedEventId
            )
        }
    }

    private fun VerificationState.computeNewVerificationState(event: Event): VerificationState {
        return when (event.getClearType()) {
            EventType.KEY_VERIFICATION_START,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_READY,
            EventType.KEY_VERIFICATION_KEY,
            EventType.KEY_VERIFICATION_MAC -> toState(VerificationState.WAITING)
            EventType.KEY_VERIFICATION_CANCEL -> toState(if (event.senderId == userId) {
                VerificationState.CANCELED_BY_ME
            } else {
                VerificationState.CANCELED_BY_OTHER
            })
            EventType.KEY_VERIFICATION_DONE -> toState(VerificationState.DONE)
            else -> VerificationState.REQUEST
        }
    }

}
