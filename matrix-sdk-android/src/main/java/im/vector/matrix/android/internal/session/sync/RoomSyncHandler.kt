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
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.helper.*
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.notification.DefaultPushRuleService
import im.vector.matrix.android.internal.session.notification.ProcessEventForPushTask
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.sync.model.*
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
                                                   private val roomEntityFactory: RoomEntityFactory,
                                                   private val chunkEntityFactory: ChunkEntityFactory,
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
        Timber.v("Execute transaction from $this")
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
                    handleInvitedRoom(realm, it.key, it.value, isInitialSync)
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
        return roomEntityFactory.create(realm, roomId, roomSync, Membership.JOIN, isInitialSync)
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync: InvitedRoomSync,
                                  isInitialSync: Boolean): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            val chunkEntity = chunkEntityFactory.create(realm, roomId, roomSync.inviteState.events, isInitialSync = isInitialSync)
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
}
