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
package org.matrix.android.sdk.internal.session.room.relation.threads

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfThread
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

/***
 * This class is responsible to Fetch paginated chunks of the thread timeline using the /relations API
 *
 * How it works
 *
 * The problem?
 *  - We cannot use the existing timeline architecture to paginate through the timeline
 *  - We want our new events to be live, so any interactions with them like reactions will continue to work. We should
 *    handle appropriately the existing events from /messages api with the new events from /relations.
 *  - Handling edge cases like receiving an event from /messages while you have already created a new one from the /relations response
 *
 * The solution
 * We generate a temporarily thread chunk that will be used to store any new paginated results from the /relations api
 * We bind the timeline events from that chunk with the already existing ones. So we will have one common instance, and
 * all reactions, edits etc will continue to work. If the events do not exists we create them
 * and we will reuse the same EventEntity instance when (and if) the same event will be fetched from the main (/messages) timeline
 *
 */
internal interface FetchThreadTimelineTask : Task<FetchThreadTimelineTask.Params, DefaultFetchThreadTimelineTask.Result> {
    data class Params(
            val roomId: String,
            val rootThreadEventId: String,
            val from: String?,
            val limit: Int

    )
}

internal class DefaultFetchThreadTimelineTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        @SessionDatabase private val monarchy: Monarchy,
        @UserId private val userId: String,
        private val cryptoService: DefaultCryptoService
) : FetchThreadTimelineTask {

    enum class Result {
        SHOULD_FETCH_MORE,
        REACHED_END,
        SUCCESS
    }

    override suspend fun execute(params: FetchThreadTimelineTask.Params): Result {
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getThreadsRelations(
                    roomId = params.roomId,
                    eventId = params.rootThreadEventId,
                    from = params.from,
                    limit = params.limit
            )
        }

        Timber.i("###THREADS FetchThreadTimelineTask Fetched size:${response.chunks.size} nextBatch:${response.nextBatch} ")
        return handleRelationsResponse(response, params)
    }

    private suspend fun handleRelationsResponse(response: RelationsResponse,
                                                params: FetchThreadTimelineTask.Params): Result {
        val threadList = response.chunks
        val threadRootEvent = response.originalEvent
        val hasReachEnd = response.nextBatch == null

        monarchy.awaitTransaction { realm ->

            val threadChunk = ChunkEntity.findLastForwardChunkOfThread(realm, params.roomId, params.rootThreadEventId)
                    ?: run {
                        return@awaitTransaction
                    }

            threadChunk.prevToken = response.nextBatch
            val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

            for (event in threadList) {
                if (event.eventId == null || event.senderId == null || event.type == null) {
                    continue
                }

                if (threadChunk.timelineEvents.find(event.eventId) != null) {
                    // Event already exists in thread chunk, skip it
                    Timber.i("###THREADS FetchThreadTimelineTask event: ${event.eventId} already exists in thread chunk, skip it")
                    continue
                }

                val timelineEvent = TimelineEventEntity
                        .where(realm, roomId = params.roomId, event.eventId)
                        .findFirst()

                if (timelineEvent != null) {
                    // Event already exists but not in the thread chunk
                    // Lets added there
                    Timber.i("###THREADS FetchThreadTimelineTask event: ${event.eventId} exists but not in the thread chunk, add it at the end")
                    threadChunk.timelineEvents.add(timelineEvent)
                } else {
                    Timber.i("###THREADS FetchThreadTimelineTask event: ${event.eventId} is brand NEW create an entity and add it!")
                    val eventEntity = createEventEntity(params.roomId, event, realm)
                    roomMemberContentsByUser.addSenderState(realm, params.roomId, event.senderId)
                    threadChunk.addTimelineEvent(
                            roomId = params.roomId,
                            eventEntity = eventEntity,
                            direction = PaginationDirection.FORWARDS,
                            ownedByThreadChunk = true,
                            roomMemberContentsByUser = roomMemberContentsByUser)
                }
            }

            if (hasReachEnd) {
                val rootThread = TimelineEventEntity
                        .where(realm, roomId = params.roomId, params.rootThreadEventId)
                        .findFirst()
                if (rootThread != null) {
                    // If root thread event already exists add it to our chunk
                    threadChunk.timelineEvents.add(rootThread)
                    Timber.i("###THREADS FetchThreadTimelineTask root thread event: ${params.rootThreadEventId} found and added!")
                } else if (threadRootEvent?.senderId != null) {
                    // Case when thread event is not in the device
                    Timber.i("###THREADS FetchThreadTimelineTask root thread event: ${params.rootThreadEventId} NOT FOUND! Lets create a temp one")
                    val eventEntity = createEventEntity(params.roomId, threadRootEvent, realm)
                    roomMemberContentsByUser.addSenderState(realm, params.roomId, threadRootEvent.senderId)
                    threadChunk.addTimelineEvent(
                            roomId = params.roomId,
                            eventEntity = eventEntity,
                            direction = PaginationDirection.FORWARDS,
                            ownedByThreadChunk = true,
                            roomMemberContentsByUser = roomMemberContentsByUser)
                }
            }
        }

        return if (hasReachEnd) {
            Result.REACHED_END
        } else {
            Result.SHOULD_FETCH_MORE
        }
    }

    // TODO Reuse this function to all the app
    /**
     * If we don't have any new state on this user, get it from db
     */
    private fun HashMap<String, RoomMemberContent?>.addSenderState(realm: Realm, roomId: String, senderId: String) {
        getOrPut(senderId) {
            CurrentStateEventEntity
                    .getOrNull(realm, roomId, senderId, EventType.STATE_ROOM_MEMBER)
                    ?.root?.asDomain()
                    ?.getFixedRoomMemberContent()
        }
    }

    /**
     * Create an EventEntity to be added in the TimelineEventEntity
     */
    private fun createEventEntity(roomId: String, event: Event, realm: Realm): EventEntity {
        val ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }
        return event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
    }

    /**
     * Invoke the event decryption mechanism for a specific event
     */
    private suspend fun decryptIfNeeded(event: Event, roomId: String) {
        try {
            // Event from sync does not have roomId, so add it to the event first
            val result = cryptoService.decryptEvent(event.copy(roomId = roomId), "")
            event.mxDecryptionResult = OlmDecryptionResult(
                    payload = result.clearEvent,
                    senderKey = result.senderCurve25519Key,
                    keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                    forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
            )
        } catch (e: MXCryptoError) {
            if (e is MXCryptoError.Base) {
                event.mCryptoError = e.errorType
                event.mCryptoErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
            }
        }
    }

    private fun handleReaction(realm: Realm,
                               event: Event,
                               roomId: String) {
        val unsignedData = event.unsignedData ?: return
        val relatedEventId = event.eventId ?: return

        unsignedData.relations?.annotations?.chunk?.forEach { relationChunk ->

            if (relationChunk.type == EventType.REACTION) {
                val reaction = relationChunk.key
                Timber.i("----> Annotation found in ${event.eventId} ${relationChunk.key} ")

                val eventSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId, relatedEventId)
                var sum = eventSummary.reactionsSummary.find { it.key == reaction }

                if (sum == null) {
                    sum = realm.createObject(ReactionAggregatedSummaryEntity::class.java)
                    sum.key = reaction
                    sum.firstTimestamp = event.originServerTs ?: 0
                    Timber.v("Adding synced reaction")
                    sum.count = 1
                    // reactionEventId not included in the /relations API
//                    sum.sourceEvents.add(reactionEventId)
                    eventSummary.reactionsSummary.add(sum)
                } else {
                    sum.count += 1
                }
            }
        }
    }
}
