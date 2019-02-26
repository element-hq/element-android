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

package im.vector.matrix.android.internal.session.user

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMembers
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.tryTransactionSync

internal interface UpdateUserTask : Task<UpdateUserTask.Params, Unit> {

    data class Params(val eventIds: List<String>)

}

internal class DefaultUpdateUserTask(private val monarchy: Monarchy) : UpdateUserTask {

    override fun execute(params: UpdateUserTask.Params): Try<Unit> {
        return monarchy.tryTransactionSync { realm ->
            params.eventIds.forEach { eventId ->
                val event = EventEntity.where(realm, eventId).findFirst()?.asDomain()
                            ?: return@forEach
                val roomId = event.roomId ?: return@forEach
                val userId = event.stateKey ?: return@forEach
                val roomMember = RoomMembers(realm, roomId).get(userId) ?: return@forEach
                if (roomMember.membership != Membership.JOIN) return@forEach

                val userEntity = UserEntity.where(realm, userId).findFirst()
                                 ?: realm.createObject(UserEntity::class.java, userId)
                userEntity.displayName = roomMember.displayName ?: ""
                userEntity.avatarUrl = roomMember.avatarUrl ?: ""
            }
        }
    }

}