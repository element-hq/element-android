/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room

import io.realm.kotlin.TypedRealm
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

internal interface RoomGetter {
    fun getRoom(roomId: String): Room?

    fun getDirectRoomWith(otherUserId: String): String?
}

@SessionScope
internal class DefaultRoomGetter @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val roomFactory: RoomFactory
) : RoomGetter {

    override fun getRoom(roomId: String): Room? {
        val realm = realmInstance.getBlockingRealm()
        return createRoom(realm, roomId)
    }

    override fun getDirectRoomWith(otherUserId: String): String? {
        val realm = realmInstance.getBlockingRealm()
        return RoomSummaryEntity.where(realm)
                .query("isDirect == true")
                .process("membershipStr", listOf(Membership.JOIN))
                .find()
                .firstOrNull { dm -> dm.otherMemberIds.size == 1 && dm.otherMemberIds.first() == otherUserId }
                ?.roomId
    }

    private fun createRoom(realm: TypedRealm, roomId: String): Room? {
        return RoomEntity.where(realm, roomId).first().find()
                ?.let { roomFactory.create(roomId) }
    }
}
