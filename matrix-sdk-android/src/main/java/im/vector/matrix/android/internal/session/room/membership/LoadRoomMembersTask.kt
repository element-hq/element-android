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

package im.vector.matrix.android.internal.session.room.membership

import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.mapper.toSQLEntity
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface LoadRoomMembersTask : Task<LoadRoomMembersTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val excludeMembership: Membership? = null
    )
}

internal class DefaultLoadRoomMembersTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val sessionDatabase: SessionDatabase,
        private val syncTokenStore: SyncTokenStore,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        private val roomMemberEventHandler: RoomMemberEventHandler,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val eventBus: EventBus
) : LoadRoomMembersTask {

    override suspend fun execute(params: LoadRoomMembersTask.Params) {
        if (sessionDatabase.roomQueries.areMembersLoaded(params.roomId).executeAsOneOrNull() == true) {
            return
        }
        val lastToken = syncTokenStore.getLastToken()
        val response = executeRequest<RoomMembersResponse>(eventBus) {
            apiCall = roomAPI.getMembers(params.roomId, lastToken, null, params.excludeMembership?.value)
        }
        insertInDb(response, params.roomId)
    }

    private suspend fun insertInDb(response: RoomMembersResponse, roomId: String) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            // We ignore all the already known members
            for (roomMemberEvent in response.roomMemberEvents) {
                if (roomMemberEvent.eventId == null || roomMemberEvent.stateKey == null) {
                    continue
                }
                val eventEntity = roomMemberEvent.toSQLEntity(roomId, SendState.SYNCED)
                sessionDatabase.eventQueries.insert(eventEntity)
                val stateEventEntity = im.vector.matrix.sqldelight.session.CurrentStateEventEntity.Impl(
                        event_id = roomMemberEvent.eventId,
                        state_key = roomMemberEvent.stateKey,
                        room_id = roomId,
                        type = roomMemberEvent.type
                )
                sessionDatabase.stateEventQueries.insertOrUpdate(stateEventEntity)
                roomMemberEventHandler.handle(roomId, roomMemberEvent)
            }
            sessionDatabase.roomQueries.setMembersAsLoaded(roomId)
            roomSummaryUpdater.update(roomId, updateMembers = true)
        }
    }

}
