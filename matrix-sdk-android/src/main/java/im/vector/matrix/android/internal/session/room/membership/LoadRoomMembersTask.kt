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

import arrow.core.Try
import com.squareup.moshi.JsonReader
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.helper.addStateEvent
import im.vector.matrix.android.internal.database.helper.updateSenderData
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.Realm
import io.realm.kotlin.createObject
import okhttp3.ResponseBody
import okio.Okio
import javax.inject.Inject

internal interface LoadRoomMembersTask : Task<LoadRoomMembersTask.Params, Boolean> {

    data class Params(
            val roomId: String,
            val excludeMembership: Membership? = null
    )
}

internal class DefaultLoadRoomMembersTask @Inject constructor(private val roomAPI: RoomAPI,
                                                              private val monarchy: Monarchy,
                                                              private val syncTokenStore: SyncTokenStore,
                                                              private val roomSummaryUpdater: RoomSummaryUpdater
) : LoadRoomMembersTask {

    override suspend fun execute(params: LoadRoomMembersTask.Params): Try<Boolean> {
        return if (areAllMembersAlreadyLoaded(params.roomId)) {
            Try.just(true)
        } else {
            val lastToken = syncTokenStore.getLastToken()
            executeRequest<RoomMembersResponse> {
                apiCall = roomAPI.getMembers(params.roomId, lastToken, null, params.excludeMembership?.value)
            }.flatMap { response ->
                insertInDb(response, params.roomId)
            }.map { true }
        }
    }

    private fun insertInDb(response: RoomMembersResponse, roomId: String): Try<Unit> {
        return monarchy
                .tryTransactionSync { realm ->
                    // We ignore all the already known members
                    val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                            ?: realm.createObject(roomId)


                    for (roomMemberEvent in response.roomMemberEvents) {
                        roomEntity.addStateEvent(roomMemberEvent)
                        UserEntityFactory.create(roomMemberEvent)?.also {
                            realm.insertOrUpdate(it)
                        }
                    }
                    roomEntity.chunks.flatMap { it.timelineEvents }.forEach {
                        it.updateSenderData()
                    }
                    roomEntity.areAllMembersLoaded = true
                    roomSummaryUpdater.update(realm, roomId)
                }
    }

    private fun areAllMembersAlreadyLoaded(roomId: String): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            RoomEntity.where(it, roomId).findFirst()?.areAllMembersLoaded ?: false
        }
    }

}
