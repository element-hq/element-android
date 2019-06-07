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

package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.tag.RoomTagContent
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.helper.lastStateIndex
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.sync.model.*
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber

internal class RoomSyncHandler(private val monarchy: Monarchy,
                               private val readReceiptHandler: ReadReceiptHandler,
                               private val roomSummaryUpdater: RoomSummaryUpdater,
                               private val roomTagHandler: RoomTagHandler,
                               private val mCrypto: CryptoManager) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(roomsSyncResponse: RoomsSyncResponse) {
        monarchy.runTransactionSync { realm ->
            handleRoomSync(realm, RoomSyncHandler.HandlingStrategy.JOINED(roomsSyncResponse.join))
            handleRoomSync(realm, RoomSyncHandler.HandlingStrategy.INVITED(roomsSyncResponse.invite))
            handleRoomSync(realm, RoomSyncHandler.HandlingStrategy.LEFT(roomsSyncResponse.leave))
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(realm: Realm, handlingStrategy: HandlingStrategy) {
        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED -> handlingStrategy.data.map { handleJoinedRoom(realm, it.key, it.value) }
            is HandlingStrategy.INVITED -> handlingStrategy.data.map { handleInvitedRoom(realm, it.key, it.value) }
            is HandlingStrategy.LEFT -> handlingStrategy.data.map { handleLeftRoom(realm, it.key, it.value) }
        }
        realm.insertOrUpdate(rooms)
    }

    private fun handleJoinedRoom(realm: Realm,
                                 roomId: String,
                                 roomSync: RoomSync): RoomEntity {

        Timber.v("Handle join sync for room $roomId")

        val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = Membership.JOIN

        val lastChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
        val isInitialSync = lastChunk == null
        val lastStateIndex = lastChunk?.lastStateIndex(PaginationDirection.FORWARDS) ?: 0
        val numberOfStateEvents = roomSync.state?.events?.size ?: 0
        val stateIndexOffset = lastStateIndex + numberOfStateEvents

        // State event
        if (roomSync.state != null && roomSync.state.events.isNotEmpty()) {
            val untimelinedStateIndex = if (isInitialSync) Int.MIN_VALUE else stateIndexOffset
            roomEntity.addStateEvents(roomSync.state.events, filterDuplicates = true, stateIndex = untimelinedStateIndex)

            // Give info to crypto module
            roomSync.state.events.forEach {
                mCrypto.onStateEvent(roomId, it)
            }
        }

        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val timelineStateOffset = if (isInitialSync || roomSync.timeline.limited.not()) 0 else stateIndexOffset
            val chunkEntity = handleTimelineEvents(
                    realm,
                    roomId,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    timelineStateOffset
            )
            roomEntity.addOrUpdate(chunkEntity)

            // Give info to crypto module
            roomSync.timeline.events.forEach {
                mCrypto.onLiveEvent(roomId, it)
            }

            // Try to remove local echo
            val transactionIds = roomSync.timeline.events.mapNotNull { it.unsignedData?.transactionId }
            transactionIds.forEach {
                val sendingEventEntity = roomEntity.sendingTimelineEvents.find(it)
                if (sendingEventEntity != null) {
                    Timber.v("Remove local echo for tx:$it")
                    roomEntity.sendingTimelineEvents.remove(sendingEventEntity)
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$it")
                }
            }
        }
        roomSummaryUpdater.update(realm, roomId, Membership.JOIN, roomSync.summary, roomSync.unreadNotifications)

        if (roomSync.ephemeral != null && roomSync.ephemeral.events.isNotEmpty()) {
            handleEphemeral(realm, roomId, roomSync.ephemeral)
        }

        if (roomSync.accountData != null && roomSync.accountData.events.isNullOrEmpty().not()) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }
        return roomEntity
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync:
                                  InvitedRoomSync): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            val chunkEntity = handleTimelineEvents(realm, roomId, roomSync.inviteState.events)
            roomEntity.addOrUpdate(chunkEntity)
        }
        roomSummaryUpdater.update(realm, roomId, Membership.INVITE)
        return roomEntity
    }

    private fun handleLeftRoom(realm: Realm,
                               roomId: String,
                               roomSync: RoomSync): RoomEntity {
        val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)

        roomEntity.membership = Membership.LEAVE
        roomEntity.chunks.deleteAllFromRealm()
        roomSummaryUpdater.update(realm, roomId, Membership.LEAVE, roomSync.summary, roomSync.unreadNotifications)
        return roomEntity
    }

    private fun handleTimelineEvents(realm: Realm,
                                     roomId: String,
                                     eventList: List<Event>,
                                     prevToken: String? = null,
                                     isLimited: Boolean = true,
                                     stateIndexOffset: Int = 0): ChunkEntity {

        val lastChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            realm.createObject<ChunkEntity>().apply { this.prevToken = prevToken }
        }
        lastChunk?.isLastForward = false
        chunkEntity.isLastForward = true
        chunkEntity.addAll(roomId, eventList, PaginationDirection.FORWARDS, stateIndexOffset)

        //update eventAnnotationSummary here?

        return chunkEntity
    }

    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeral: RoomSyncEphemeral) {
        ephemeral.events
                .filter { it.getClearType() == EventType.RECEIPT }
                .map { it.content.toModel<ReadReceiptContent>() }
                .forEach { readReceiptHandler.handle(realm, roomId, it) }
    }

    private fun handleRoomAccountDataEvents(realm: Realm, roomId: String, accountData: RoomSyncAccountData) {
        accountData.events
                .filter { it.getClearType() == EventType.TAG }
                .map { it.content.toModel<RoomTagContent>() }
                .forEach { roomTagHandler.handle(realm, roomId, it) }
    }

}