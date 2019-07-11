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
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.tag.RoomTagContent
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.helper.addStateEvent
import im.vector.matrix.android.internal.database.helper.addStateEvents
import im.vector.matrix.android.internal.database.helper.lastStateIndex
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.notification.DefaultPushRuleService
import im.vector.matrix.android.internal.session.notification.ProcessEventForPushTask
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.sync.model.*
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val monarchy: Monarchy,
                                                   private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomTagHandler: RoomTagHandler,
                                                   private val cryptoManager: CryptoManager,
                                                   private val tokenStore: SyncTokenStore,
                                                   private val pushRuleService: DefaultPushRuleService,
                                                   private val processForPushTask: ProcessEventForPushTask,
                                                   private val taskExecutor: TaskExecutor) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(roomsSyncResponse: RoomsSyncResponse, reporter: DefaultInitialSyncProgressService? = null) {
        monarchy.runTransactionSync { realm ->
            handleRoomSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), reporter)
            handleRoomSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), reporter)
            handleRoomSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), reporter)
        }

        //handle event for bing rule checks
        checkPushRules(roomsSyncResponse)

    }

    private fun checkPushRules(roomsSyncResponse: RoomsSyncResponse) {
        Timber.v("[PushRules] --> checkPushRules")
        if (tokenStore.getLastToken() == null) {
            Timber.v("[PushRules] <-- No push tule check on initial sync")
            return
        } //nothing on initial sync

        val rules = pushRuleService.getPushRules("global")
        processForPushTask.configureWith(ProcessEventForPushTask.Params(roomsSyncResponse, rules))
                .executeBy(taskExecutor)
        Timber.v("[PushRules] <-- Push task scheduled")
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(realm: Realm, handlingStrategy: HandlingStrategy, reporter: DefaultInitialSyncProgressService?) {

        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_joined_rooms, 0.6f) {
                    handleJoinedRoom(realm, it.key, it.value)
                }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_invited_rooms, 0.4f) {
                    handleInvitedRoom(realm, it.key, it.value)
                }

            is HandlingStrategy.LEFT    -> {
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_left_rooms, 0.2f) {
                    handleLeftRoom(realm, it.key, it.value)
                }
            }
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
            val userEntities = ArrayList<UserEntity>(roomSync.state.events.size)
            val untimelinedStateIndex = if (isInitialSync) Int.MIN_VALUE else stateIndexOffset
            roomSync.state.events.forEach { event ->
                roomEntity.addStateEvent(event, filterDuplicates = true, stateIndex = untimelinedStateIndex)
                // Give info to crypto module
                cryptoManager.onStateEvent(roomId, event)
                UserEntityFactory.create(event)?.also {
                    userEntities.add(it)
                }
            }
            realm.insertOrUpdate(userEntities)
        }

        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val timelineStateOffset = if (isInitialSync || roomSync.timeline.limited.not()) 0 else stateIndexOffset
            val chunkEntity = handleTimelineEvents(
                    realm,
                    roomEntity,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    timelineStateOffset
            )
            roomEntity.addOrUpdate(chunkEntity)
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
            val chunkEntity = handleTimelineEvents(realm, roomEntity, roomSync.inviteState.events)
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
                                     roomEntity: RoomEntity,
                                     eventList: List<Event>,
                                     prevToken: String? = null,
                                     isLimited: Boolean = true,
                                     stateIndexOffset: Int = 0): ChunkEntity {

        val lastChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomEntity.roomId)
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            realm.createObject<ChunkEntity>().apply { this.prevToken = prevToken }
        }
        lastChunk?.isLastForward = false
        chunkEntity.isLastForward = true

        val userEntities = ArrayList<UserEntity>(eventList.size)
        for (event in eventList) {
            chunkEntity.add(roomEntity.roomId, event, PaginationDirection.FORWARDS, stateIndexOffset)
            // Give info to crypto module
            cryptoManager.onLiveEvent(roomEntity.roomId, event)
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
            UserEntityFactory.create(event)?.also {
                userEntities.add(it)
            }
        }
        realm.insertOrUpdate(userEntities)
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
