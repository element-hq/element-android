/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.updateThreadSummaryIfNeeded
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

internal interface FetchThreadTimelineTask : Task<FetchThreadTimelineTask.Params, Boolean> {
    data class Params(
            val roomId: String,
            val rootThreadEventId: String
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

    override suspend fun execute(params: FetchThreadTimelineTask.Params): Boolean {
        val isRoomEncrypted = cryptoSessionInfoProvider.isRoomEncrypted(params.roomId)
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getRelations(
                    roomId = params.roomId,
                    eventId = params.rootThreadEventId,
                    relationType = RelationType.IO_THREAD,
                    eventType = if (isRoomEncrypted) EventType.ENCRYPTED else EventType.MESSAGE,
                    limit = 2000
            )
        }

        val threadList = response.chunks + listOfNotNull(response.originalEvent)

        return storeNewEventsIfNeeded(threadList, params.roomId)
    }

    /**
     * Store new events if they are not already received, and returns weather or not,
     * a timeline update should be made
     * @param threadList is the list containing the thread replies
     * @param roomId the roomId of the the thread
     * @return
     */
    private suspend fun storeNewEventsIfNeeded(threadList: List<Event>, roomId: String): Boolean {
        var eventsSkipped = 0
        monarchy
                .awaitTransaction { realm ->
                    val chunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)

                    val optimizedThreadSummaryMap = hashMapOf<String, EventEntity>()
                    val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

                    for (event in threadList.reversed()) {
                        if (event.eventId == null || event.senderId == null || event.type == null) {
                            eventsSkipped++
                            continue
                        }

                        if (EventEntity.where(realm, event.eventId).findFirst() != null) {
                            //  Skip if event already exists
                            eventsSkipped++
                            continue
                        }
                        if (event.isEncrypted()) {
                            // Decrypt events that will be stored
                            decryptIfNeeded(event, roomId)
                        }

                        handleReaction(realm, event, roomId)

                        val ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }
                        val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)

                        // Sender info
                        roomMemberContentsByUser.getOrPut(event.senderId) {
                            // If we don't have any new state on this user, get it from db
                            val rootStateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, event.senderId, EventType.STATE_ROOM_MEMBER)?.root
                            rootStateEvent?.asDomain()?.getFixedRoomMemberContent()
                        }

                        chunk?.addTimelineEvent(roomId, eventEntity, PaginationDirection.FORWARDS, roomMemberContentsByUser)
                        eventEntity.rootThreadEventId?.let {
                            // This is a thread event
                            optimizedThreadSummaryMap[it] = eventEntity
                        } ?: run {
                            // This is a normal event or a root thread one
                            optimizedThreadSummaryMap[eventEntity.eventId] = eventEntity
                        }
                    }

                    optimizedThreadSummaryMap.updateThreadSummaryIfNeeded(
                            roomId = roomId,
                            realm = realm,
                            currentUserId = userId,
                            shouldUpdateNotifications = false
                    )
                }
        Timber.i("----> size: ${threadList.size} | skipped: $eventsSkipped | threads: ${threadList.map { it.eventId }}")

        return eventsSkipped == threadList.size
    }

    /**
     * Invoke the event decryption mechanism for a specific event
     */

    private fun decryptIfNeeded(event: Event, roomId: String) {
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
                    Timber.v("Adding synced reaction $reaction")
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
