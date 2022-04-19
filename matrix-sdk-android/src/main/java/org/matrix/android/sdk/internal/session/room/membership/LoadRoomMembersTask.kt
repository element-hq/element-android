/*
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

package org.matrix.android.sdk.internal.session.room.membership

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.kotlin.createObject
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.sync.SyncTokenStore
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface LoadRoomMembersTask : Task<LoadRoomMembersTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val excludeMembership: Membership? = null
    )
}

internal class DefaultLoadRoomMembersTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val syncTokenStore: SyncTokenStore,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        private val roomMemberEventHandler: RoomMemberEventHandler,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val deviceListManager: DeviceListManager,
        private val globalErrorReceiver: GlobalErrorReceiver
) : LoadRoomMembersTask {

    override suspend fun execute(params: LoadRoomMembersTask.Params) {
        when (getRoomMembersLoadStatus(params.roomId)) {
            RoomMembersLoadStatusType.NONE    -> doRequest(params)
            RoomMembersLoadStatusType.LOADING -> waitPreviousRequestToFinish(params)
            RoomMembersLoadStatusType.LOADED  -> Unit
        }
    }

    private suspend fun waitPreviousRequestToFinish(params: LoadRoomMembersTask.Params) {
        try {
            awaitNotEmptyResult(monarchy.realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomEntity::class.java)
                        .equalTo(RoomEntityFields.ROOM_ID, params.roomId)
                        .equalTo(RoomEntityFields.MEMBERS_LOAD_STATUS_STR, RoomMembersLoadStatusType.LOADED.name)
            }
        } catch (exception: TimeoutCancellationException) {
            // Timeout, do the request anyway (?)
            doRequest(params)
        }
    }

    private suspend fun doRequest(params: LoadRoomMembersTask.Params) {
        setRoomMembersLoadStatus(params.roomId, RoomMembersLoadStatusType.LOADING)

        val lastToken = syncTokenStore.getLastToken()
        val response = try {
            executeRequest(globalErrorReceiver) {
                roomAPI.getMembers(params.roomId, lastToken, null, params.excludeMembership)
            }
        } catch (throwable: Throwable) {
            // Revert status to NONE
            setRoomMembersLoadStatus(params.roomId, RoomMembersLoadStatusType.NONE)
            throw throwable
        }
        // This will also set the status to LOADED
        insertInDb(response, params.roomId)
    }

    private suspend fun insertInDb(response: RoomMembersResponse, roomId: String) {
        monarchy.awaitTransaction { realm ->
            // We ignore all the already known members
            val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                    ?: realm.createObject(roomId)
            val now = System.currentTimeMillis()
            for (roomMemberEvent in response.roomMemberEvents) {
                if (roomMemberEvent.eventId == null || roomMemberEvent.stateKey == null || roomMemberEvent.type == null) {
                    continue
                }
                val ageLocalTs = roomMemberEvent.unsignedData?.age?.let { now - it }
                val eventEntity = roomMemberEvent.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
                CurrentStateEventEntity.getOrCreate(
                        realm,
                        roomId,
                        roomMemberEvent.stateKey,
                        roomMemberEvent.type
                ).apply {
                    eventId = roomMemberEvent.eventId
                    root = eventEntity
                }
                roomMemberEventHandler.handle(realm, roomId, roomMemberEvent, false)
            }
            roomEntity.membersLoadStatus = RoomMembersLoadStatusType.LOADED
            roomSummaryUpdater.update(realm, roomId, updateMembers = true)
        }

        if (cryptoSessionInfoProvider.isRoomEncrypted(roomId)) {
            deviceListManager.onRoomMembersLoadedFor(roomId)
        }
    }

    private fun getRoomMembersLoadStatus(roomId: String): RoomMembersLoadStatusType {
        var result: RoomMembersLoadStatusType?
        Realm.getInstance(monarchy.realmConfiguration).use {
            result = RoomEntity.where(it, roomId).findFirst()?.membersLoadStatus
        }
        return result ?: RoomMembersLoadStatusType.NONE
    }

    private suspend fun setRoomMembersLoadStatus(roomId: String, status: RoomMembersLoadStatusType) {
        monarchy.awaitTransaction { realm ->
            val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
            roomEntity.membersLoadStatus = status
        }
    }
}
