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

package im.vector.matrix.android.internal.session.room.alias

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.findByAlias
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import io.realm.Realm
import javax.inject.Inject

internal interface GetRoomIdByAliasTask : Task<GetRoomIdByAliasTask.Params, String?> {
    data class Params(
            val roomAlias: String
    )
}

internal class DefaultGetRoomIdByAliasTask @Inject constructor(private val monarchy: Monarchy,
                                                               private val roomAPI: RoomAPI) : GetRoomIdByAliasTask {

    override suspend fun execute(params: GetRoomIdByAliasTask.Params): String? {
        val roomId = Realm.getInstance(monarchy.realmConfiguration).use {
            RoomSummaryEntity.findByAlias(it, params.roomAlias)?.roomId
        }
        if (roomId != null) {
            return roomId
        }
        return executeRequest<RoomAliasDescription> {
            apiCall = roomAPI.getRoomIdByAlias(params.roomAlias)
        }.roomId
    }
}
