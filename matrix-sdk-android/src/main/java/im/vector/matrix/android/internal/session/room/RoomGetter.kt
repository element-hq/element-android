/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import io.realm.Realm
import javax.inject.Inject

internal interface RoomGetter {
    fun getRoom(roomId: String): Room?

    fun getDirectRoomWith(otherUserId: String): Room?
}

@SessionScope
internal class DefaultRoomGetter @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val roomFactory: RoomFactory
) : RoomGetter {

    override fun getRoom(roomId: String): Room? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            createRoom(realm, roomId)
        }
    }

    override fun getDirectRoomWith(otherUserId: String): Room? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            RoomSummaryEntity.where(realm)
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                    .equalTo(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
                    .findAll()
                    .filter { dm -> dm.otherMemberIds.contains(otherUserId) }
                    .map { it.roomId }
                    .firstOrNull { roomId -> otherUserId in RoomMemberHelper(realm, roomId).getActiveRoomMemberIds() }
                    ?.let { roomId -> createRoom(realm, roomId) }
        }
    }

    private fun createRoom(realm: Realm, roomId: String): Room? {
        return RoomEntity.where(realm, roomId).findFirst()
                ?.let { roomFactory.create(roomId) }
    }
}
