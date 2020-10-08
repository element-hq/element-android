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

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import io.realm.Realm
import javax.inject.Inject

internal class RoomMemberEventHandler @Inject constructor() {

    fun handle(realm: Realm, roomId: String, event: Event): Boolean {
        if (event.type != EventType.STATE_ROOM_MEMBER) {
            return false
        }
        val userId = event.stateKey ?: return false
        val roomMember = event.content.toModel<RoomMemberContent>()
        return handle(realm, roomId, userId, roomMember)
    }

    fun handle(realm: Realm, roomId: String, userId: String, roomMember: RoomMemberContent?): Boolean {
        if (roomMember == null) {
            return false
        }
        val roomMemberEntity = RoomMemberEntityFactory.create(roomId, userId, roomMember)
        realm.insertOrUpdate(roomMemberEntity)
        if (roomMember.membership.isActive()) {
            val userEntity = UserEntityFactory.create(userId, roomMember)
            realm.insertOrUpdate(userEntity)
        }
        return true
    }
}
