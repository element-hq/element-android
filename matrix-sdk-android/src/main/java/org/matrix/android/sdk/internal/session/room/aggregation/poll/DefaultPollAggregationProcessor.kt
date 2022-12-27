/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.aggregation.poll

import io.realm.Realm
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.PollSummaryContent
import org.matrix.android.sdk.api.session.room.model.VoteInfo
import org.matrix.android.sdk.api.session.room.model.VoteSummary
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.create
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.room.relation.poll.FetchPollResponseEventsTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import javax.inject.Inject

internal class DefaultPollAggregationProcessor @Inject constructor(
        private val taskExecutor: TaskExecutor,
        private val fetchPollResponseEventsTask: FetchPollResponseEventsTask,
) : PollAggregationProcessor {

    override fun handlePollStartEvent(realm: Realm, event: Event): Boolean {
        val content = event.getClearContent()?.toModel<MessagePollContent>()
        if (content?.relatesTo?.type != RelationType.REPLACE) {
            return false
        }

        val roomId = event.roomId ?: return false
        val targetEventId = content.relatesTo.eventId ?: return false

        EventAnnotationsSummaryEntity.getOrCreate(realm, roomId, targetEventId).let { eventAnnotationsSummaryEntity ->
            ContentMapper
                    .map(eventAnnotationsSummaryEntity.pollResponseSummary?.aggregatedContent)
                    ?.toModel<PollSummaryContent>()
                    ?.let { existingPollSummaryContent ->
                        eventAnnotationsSummaryEntity.pollResponseSummary?.aggregatedContent = ContentMapper.map(
                                PollSummaryContent(
                                        myVote = existingPollSummaryContent.myVote,
                                        votes = emptyList(),
                                        votesSummary = emptyMap(),
                                        totalVotes = 0,
                                        winnerVoteCount = 0,
                                )
                                        .toContent()
                        )
                    }
        }
        return true
    }

    override fun handlePollResponseEvent(session: Session, realm: Realm, event: Event): Boolean {
        val content = event.getClearContent()?.toModel<MessagePollResponseContent>() ?: return false
        val roomId = event.roomId ?: return false
        val senderId = event.senderId ?: return false
        val targetEventId = event.getRelationContent()?.eventId ?: return false
        val targetPollContent = getPollContent(session, roomId, targetEventId) ?: return false

        val annotationsSummaryEntity = getAnnotationsSummaryEntity(realm, roomId, targetEventId)
        val aggregatedPollSummaryEntity = getAggregatedPollSummaryEntity(realm, annotationsSummaryEntity)

        val closedTime = aggregatedPollSummaryEntity.closedTime
        val responseTime = event.originServerTs ?: return false
        if (closedTime != null && responseTime > closedTime) {
            return false
        }

        if (aggregatedPollSummaryEntity.sourceEvents.contains(event.eventId)) {
            return false
        }

        val txId = event.unsignedData?.transactionId
        val isLocalEcho = LocalEcho.isLocalEchoId(event.eventId ?: "")
        if (!isLocalEcho && aggregatedPollSummaryEntity.sourceLocalEchoEvents.contains(txId)) {
            aggregatedPollSummaryEntity.sourceLocalEchoEvents.remove(txId)
            aggregatedPollSummaryEntity.sourceEvents.add(event.eventId)
            return false
        }

        val vote = content.getBestResponse()?.answers?.first() ?: return false
        if (!targetPollContent.getBestPollCreationInfo()?.answers?.map { it.id }?.contains(vote).orFalse()) {
            return false
        }

        val pollSummaryModel = ContentMapper.map(aggregatedPollSummaryEntity.aggregatedContent).toModel<PollSummaryContent>()
        val existingVotes = pollSummaryModel?.votes.orEmpty().toMutableList()
        val existingVoteIndex = existingVotes.indexOfFirst { it.userId == senderId }

        if (existingVoteIndex != -1) {
            val existingVote = existingVotes[existingVoteIndex]
            if (existingVote.voteTimestamp > responseTime) {
                return false
            }
            existingVotes[existingVoteIndex] = VoteInfo(senderId, vote, responseTime)
        } else {
            existingVotes.add(VoteInfo(senderId, vote, responseTime))
        }

        // Precompute the percentage of votes for all options
        val totalVotes = existingVotes.size
        val newVotesSummary = existingVotes
                .groupBy({ it.option }, { it.userId })
                .mapValues {
                    VoteSummary(
                            total = it.value.size,
                            percentage = if (totalVotes == 0 && it.value.isEmpty()) 0.0 else it.value.size.toDouble() / totalVotes
                    )
                }
        val newWinnerVoteCount = newVotesSummary.maxOf { it.value.total }

        if (isLocalEcho) {
            aggregatedPollSummaryEntity.sourceLocalEchoEvents.add(event.eventId)
        } else {
            aggregatedPollSummaryEntity.sourceEvents.add(event.eventId)
        }

        val myVote = existingVotes.find { it.userId == session.myUserId }?.option

        val newSumModel = PollSummaryContent(
                myVote = myVote,
                votes = existingVotes,
                votesSummary = newVotesSummary,
                totalVotes = totalVotes,
                winnerVoteCount = newWinnerVoteCount
        )
        aggregatedPollSummaryEntity.aggregatedContent = ContentMapper.map(newSumModel.toContent())

        event.eventId?.let { removeEncryptedRelatedEventIdIfNeeded(aggregatedPollSummaryEntity, it) }

        return true
    }

    override fun handlePollEndEvent(session: Session, powerLevelsHelper: PowerLevelsHelper, realm: Realm, event: Event): Boolean {
        val roomId = event.roomId ?: return false
        val pollEventId = event.getRelationContent()?.eventId ?: return false
        val pollOwnerId = getPollEvent(session, roomId, pollEventId)?.root?.senderId
        val isPollOwner = pollOwnerId == event.senderId

        if (!isPollOwner && !powerLevelsHelper.isUserAbleToRedact(event.senderId ?: "")) {
            return false
        }

        val annotationsSummaryEntity = getAnnotationsSummaryEntity(realm, roomId, pollEventId)
        val aggregatedPollSummaryEntity = getAggregatedPollSummaryEntity(realm, annotationsSummaryEntity)

        val txId = event.unsignedData?.transactionId
        aggregatedPollSummaryEntity.closedTime = event.originServerTs

        val isLocalEcho = LocalEcho.isLocalEchoId(event.eventId ?: "")
        if (!isLocalEcho && aggregatedPollSummaryEntity.sourceLocalEchoEvents.contains(txId)) {
            aggregatedPollSummaryEntity.sourceLocalEchoEvents.remove(txId)
            aggregatedPollSummaryEntity.sourceEvents.add(event.eventId)
        }

        event.eventId?.let { removeEncryptedRelatedEventIdIfNeeded(aggregatedPollSummaryEntity, it) }

        if (!isLocalEcho) {
            ensurePollIsFullyAggregated(roomId, pollEventId)
        }

        return true
    }

    private fun getPollEvent(session: Session, roomId: String, eventId: String): TimelineEvent? {
        return session.roomService().getRoom(roomId)?.getTimelineEvent(eventId)
    }

    private fun getPollContent(session: Session, roomId: String, eventId: String): MessagePollContent? {
        val pollEvent = getPollEvent(session, roomId, eventId)
        return pollEvent?.getLastMessageContent() as? MessagePollContent
    }

    private fun getAnnotationsSummaryEntity(realm: Realm, roomId: String, eventId: String): EventAnnotationsSummaryEntity {
        return EventAnnotationsSummaryEntity.where(realm, roomId, eventId).findFirst()
                ?: EventAnnotationsSummaryEntity.create(realm, roomId, eventId)
    }

    private fun getAggregatedPollSummaryEntity(
            realm: Realm,
            eventAnnotationsSummaryEntity: EventAnnotationsSummaryEntity
    ): PollResponseAggregatedSummaryEntity {
        return eventAnnotationsSummaryEntity.pollResponseSummary
                ?: realm.createObject(PollResponseAggregatedSummaryEntity::class.java).also {
                    eventAnnotationsSummaryEntity.pollResponseSummary = it
                }
    }

    /**
     * Check that all related votes to a given poll are all retrieved and aggregated.
     */
    private fun ensurePollIsFullyAggregated(
            roomId: String,
            pollEventId: String
    ) {
        taskExecutor.executorScope.launch {
            val params = FetchPollResponseEventsTask.Params(
                    roomId = roomId,
                    startPollEventId = pollEventId,
            )
            fetchPollResponseEventsTask.execute(params)
        }
    }

    private fun removeEncryptedRelatedEventIdIfNeeded(aggregatedPollSummaryEntity: PollResponseAggregatedSummaryEntity, eventId: String) {
        if (aggregatedPollSummaryEntity.encryptedRelatedEventIds.contains(eventId)) {
            aggregatedPollSummaryEntity.encryptedRelatedEventIds.remove(eventId)
        }
    }
}
