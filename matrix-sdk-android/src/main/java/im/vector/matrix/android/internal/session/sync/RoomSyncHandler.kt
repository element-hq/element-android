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
import im.vector.matrix.android.R
import im.vector.matrix.android.api.pushrules.RuleScope
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
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.notification.DefaultPushRuleService
import im.vector.matrix.android.internal.session.notification.ProcessEventForPushTask
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.read.FullyReadContent
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.sync.model.*
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val monarchy: Monarchy,
                                                   private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomTagHandler: RoomTagHandler,
                                                   private val roomFullyReadHandler: RoomFullyReadHandler,
                                                   private val cryptoService: DefaultCryptoService,
                                                   private val tokenStore: SyncTokenStore,
                                                   private val pushRuleService: DefaultPushRuleService,
                                                   private val processForPushTask: ProcessEventForPushTask,
                                                   private val taskExecutor: TaskExecutor) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    suspend fun handle(roomsSyncResponse: RoomsSyncResponse, isInitialSync: Boolean, reporter: DefaultInitialSyncProgressService? = null) {
        monarchy.awaitTransaction { realm ->
            handleRoomSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), isInitialSync, reporter)
            handleRoomSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), isInitialSync, reporter)
            handleRoomSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), isInitialSync, reporter)
        }
        // handle event for bing rule checks
        checkPushRules(roomsSyncResponse)
    }

    private fun checkPushRules(roomsSyncResponse: RoomsSyncResponse) {
        Timber.v("[PushRules] --> checkPushRules")
        if (tokenStore.getLastToken() == null) {
            Timber.v("[PushRules] <-- No push rule check on initial sync")
            return
        } // nothing on initial sync

        val rules = pushRuleService.getPushRules(RuleScope.GLOBAL)
        processForPushTask.configureWith(ProcessEventForPushTask.Params(roomsSyncResponse, rules))
                .executeBy(taskExecutor)
        Timber.v("[PushRules] <-- Push task scheduled")
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(realm: Realm, handlingStrategy: HandlingStrategy, isInitialSync: Boolean, reporter: DefaultInitialSyncProgressService?) {

        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_joined_rooms, 0.6f) {
                    handleJoinedRoom(realm, it.key, it.value, isInitialSync)
                }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_invited_rooms, 0.1f) {
                    handleInvitedRoom(realm, it.key, it.value)
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
                                 isInitialSync: Boolean): RoomEntity {

        Timber.v("Handle join sync for room $roomId")

        if (roomSync.ephemeral != null && roomSync.ephemeral.events.isNotEmpty()) {
            handleEphemeral(realm, roomId, roomSync.ephemeral, isInitialSync)
        }

        if (roomSync.accountData != null && roomSync.accountData.events.isNullOrEmpty().not()) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }

        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = Membership.JOIN

        // State event
        if (roomSync.state != null && roomSync.state.events.isNotEmpty()) {
            val minStateIndex = roomEntity.untimelinedStateEvents.where().min(EventEntityFields.STATE_INDEX)?.toInt()
                                ?: Int.MIN_VALUE
            val untimelinedStateIndex = minStateIndex + 1
            roomSync.state.events.forEach { event ->
                roomEntity.addStateEvent(event, filterDuplicates = true, stateIndex = untimelinedStateIndex)
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                UserEntityFactory.createOrNull(event)?.also {
                    realm.insertOrUpdate(it)
                }
            }
        }

        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val chunkEntity = handleTimelineEvents(
                    realm,
                    roomEntity,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited
            )
            roomEntity.addOrUpdate(chunkEntity)
        }
        roomSummaryUpdater.update(realm, roomId, Membership.JOIN, roomSync.summary, roomSync.unreadNotifications)
        return roomEntity
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync:
                                  InvitedRoomSync): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            val chunkEntity = handleTimelineEvents(realm, roomEntity, roomSync.inviteState.events)
            roomEntity.addOrUpdate(chunkEntity)
        }
        roomSummaryUpdater.update(realm, roomId, Membership.INVITE)
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
                                     isLimited: Boolean = true): ChunkEntity {

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

        val eventIds = ArrayList<String>(eventList.size)
        for (event in eventList) {
            event.eventId?.also { eventIds.add(it) }
            chunkEntity.add(roomEntity.roomId, event, PaginationDirection.FORWARDS, stateIndexOffset)
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
            UserEntityFactory.createOrNull(event)?.also {
                realm.insertOrUpdate(it)
            }
        }
        chunkEntity.updateSenderDataFor(eventIds)
        return chunkEntity
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeral: RoomSyncEphemeral,
                                isInitialSync: Boolean) {
        for (event in ephemeral.events) {
            if (event.type != EventType.RECEIPT) continue
            val readReceiptContent = event.content as? ReadReceiptContent ?: continue
            readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync)
        }
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
