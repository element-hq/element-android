/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.room.members

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.members.RoomMembersService
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchCopied

internal class DefaultRoomMembersService(private val roomId: String,
                                         private val monarchy: Monarchy,
                                         private val loadRoomMembersTask: LoadRoomMembersTask,
                                         private val taskExecutor: TaskExecutor
) : RoomMembersService {

    override fun loadRoomMembersIfNeeded(): Cancelable {
        val params = LoadRoomMembersTask.Params(roomId, Membership.LEAVE)
        return loadRoomMembersTask.configureWith(params).executeBy(taskExecutor)
    }

    override fun getRoomMember(userId: String): RoomMember? {
        val eventEntity = monarchy.fetchCopied {
            RoomMembers(it, roomId).queryRoomMemberEvent(userId).findFirst()
        }
        return eventEntity?.asDomain()?.content.toModel()
    }

    override fun getRoomMemberIdsLive(): LiveData<List<String>> {
        return monarchy.findAllMappedWithChanges(
                {
                    RoomMembers(it, roomId).queryRoomMembersEvent()
                },
                {
                    it.stateKey!!
                }
        )
    }
}