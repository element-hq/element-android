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

import im.vector.matrix.android.R
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.tag.RoomTagContent
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.helper.*
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.membership.RoomMemberEventHandler
import im.vector.matrix.android.internal.session.room.read.FullyReadContent
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.typing.TypingEventContent
import im.vector.matrix.android.internal.session.sync.model.*
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomTagHandler: RoomTagHandler,
                                                   private val roomFullyReadHandler: RoomFullyReadHandler,
                                                   private val cryptoService: DefaultCryptoService,
                                                   private val roomMemberEventHandler: RoomMemberEventHandler,
                                                   private val timelineEventSenderVisitor: TimelineEventSenderVisitor) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(
            realm: Realm,
            roomsSyncResponse: RoomsSyncResponse,
            isInitialSync: Boolean,
            reporter: DefaultInitialSyncProgressService? = null
    ) {
        Timber.v("Execute transaction from $this")
        handleRoomSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), isInitialSync, reporter)
        handleRoomSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), isInitialSync, reporter)
        handleRoomSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), isInitialSync, reporter)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(realm: Realm, handlingStrategy: HandlingStrategy, isInitialSync: Boolean, reporter: DefaultInitialSyncProgressService?) {
        val syncLocalTimeStampMillis = System.currentTimeMillis()
        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_joined_rooms, 0.6f) {
                    handleJoinedRoom(realm, it.key, it.value, isInitialSync, syncLocalTimeStampMillis)
                }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_invited_rooms, 0.1f) {
                    handleInvitedRoom(realm, it.key, it.value, syncLocalTimeStampMillis)
                }

            is HandlingStrategy.LEFT    -> {
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_left_rooms, 0.3f) {
                    handleLeftRoom(realm, it.key, it.value)
                }
            }
        }
        realm.insertOrUpdate(rooms)
    }

    private fun handleJoinedRoom(realm: Realm,
                                 roomId: String,
                                 roomSync: RoomSync,
                                 isInitialSync: Boolean,
                                 syncLocalTimestampMillis: Long): RoomEntity {
        Timber.v("Handle join sync for room $roomId")

        var ephemeralResult: EphemeralResult? = null
        if (roomSync.ephemeral?.events?.isNotEmpty() == true) {
            ephemeralResult = handleEphemeral(realm, roomId, roomSync.ephemeral, isInitialSync)
        }

        if (roomSync.accountData?.events?.isNotEmpty() == true) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }

        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = Membership.JOIN

        // State event

        if (roomSync.state?.events?.isNotEmpty() == true) {
            val minStateIndex = roomEntity.untimelinedStateEvents.where().min(EventEntityFields.STATE_INDEX)?.toInt()
                    ?: Int.MIN_VALUE
            val untimelinedStateIndex = minStateIndex + 1
            roomSync.state.events.forEach { event ->
                roomEntity.addStateEvent(event, filterDuplicates = true, stateIndex = untimelinedStateIndex)
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                roomMemberEventHandler.handle(realm, roomId, event)
            }
        }
        if (roomSync.timeline?.events?.isNotEmpty() == true) {
            val chunkEntity = handleTimelineEvents(
                    realm,
                    roomEntity,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    syncLocalTimestampMillis
            )
            roomEntity.addOrUpdate(chunkEntity)
        }
        val hasRoomMember = roomSync.state?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null || roomSync.timeline?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null

        roomSummaryUpdater.update(
                realm,
                roomId,
                Membership.JOIN,
                roomSync.summary,
                roomSync.unreadNotifications,
                updateMembers = hasRoomMember,
                ephemeralResult = ephemeralResult)
        return roomEntity
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync: InvitedRoomSync,
                                  syncLocalTimestampMillis: Long): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            val chunkEntity = handleTimelineEvents(realm, roomEntity, roomSync.inviteState.events, syncLocalTimestampMillis = syncLocalTimestampMillis)
            roomEntity.addOrUpdate(chunkEntity)
        }
        val hasRoomMember = roomSync.inviteState?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null
        roomSummaryUpdater.update(realm, roomId, Membership.INVITE, updateMembers = hasRoomMember)
        return roomEntity
    }

    private fun handleLeftRoom(realm: Realm,
                               roomId: String,
                               roomSync: RoomSync): RoomEntity {
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

        roomEntity.membership = Membership.LEAVE
        roomEntity.chunks.deleteAllFromRealm()
        roomSummaryUpdater.update(realm, roomId, Membership.LEAVE, roomSync.summary, roomSync.unreadNotifications)
        return roomEntity
    }

    private fun handleTimelineEvents(realm: Realm,
                                     roomEntity: RoomEntity,
                                     eventList: List<Event>,
                                     prevToken: String? = null,
                                     isLimited: Boolean = true,
                                     syncLocalTimestampMillis: Long): ChunkEntity {
        val lastChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomEntity.roomId)
        var stateIndexOffset = 0
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            realm.createObject<ChunkEntity>().apply { this.prevToken = prevToken }
        }
        if (isLimited && lastChunk != null) {
            stateIndexOffset = lastChunk.lastStateIndex(PaginationDirection.FORWARDS)
        }
        lastChunk?.isLastForward = false
        chunkEntity.isLastForward = true
        chunkEntity.isUnlinked = false

        val timelineEvents = ArrayList<TimelineEventEntity>(eventList.size)
        for (event in eventList) {
            event.ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            chunkEntity.add(roomEntity.roomId, event, PaginationDirection.FORWARDS, stateIndexOffset)?.also {
                timelineEvents.add(it)
            }
            // Give info to crypto module
            cryptoService.onLiveEvent(roomEntity.roomId, event)
            // Try to remove local echo
            event.unsignedData?.transactionId?.also {
                val sendingEventEntity = roomEntity.sendingTimelineEvents.find(it)
                if (sendingEventEntity != null) {
                    Timber.v("Remove local echo for tx:$it")
                    roomEntity.sendingTimelineEvents.remove(sendingEventEntity)
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$it")
                }
            }
            roomMemberEventHandler.handle(realm, roomEntity.roomId, event)
        }
        timelineEventSenderVisitor.visit(timelineEvents)
        return chunkEntity
    }

    data class EphemeralResult(
            val typingUserIds: List<String> = emptyList()
    )

    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeral: RoomSyncEphemeral,
                                isInitialSync: Boolean): EphemeralResult {
        var result = EphemeralResult()
        for (event in ephemeral.events) {
            when (event.type) {
                EventType.RECEIPT -> {
                    @Suppress("UNCHECKED_CAST")
                    (event.content as? ReadReceiptContent)?.let { readReceiptContent ->
                        readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync)
                    }
                }
                EventType.TYPING  -> {
                    event.content.toModel<TypingEventContent>()?.let { typingEventContent ->
                        result = result.copy(typingUserIds = typingEventContent.typingUserIds)
                    }
                }
                else              -> Timber.w("Ephemeral event type '${event.type}' not yet supported")
            }
        }

        return result
    }

    private fun handleRoomAccountDataEvents(realm: Realm, roomId: String, accountData: RoomSyncAccountData) {
        for (event in accountData.events) {
            val eventType = event.getClearType()
            if (eventType == EventType.TAG) {
                val content = event.getClearContent().toModel<RoomTagContent>()
                roomTagHandler.handle(realm, roomId, content)
            } else if (eventType == EventType.FULLY_READ) {
                val content = event.getClearContent().toModel<FullyReadContent>()
                roomFullyReadHandler.handle(realm, roomId, content)
            }
        }
    }
}
