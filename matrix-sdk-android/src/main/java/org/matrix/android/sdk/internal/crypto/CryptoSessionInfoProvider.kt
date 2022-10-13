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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import javax.inject.Inject

/**
 * The crypto module needs some information regarding rooms that are stored
 * in the session DB, this class encapsulate this functionality.
 */
internal class CryptoSessionInfoProvider @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
) {

    fun isRoomEncrypted(roomId: String): Boolean {
        // We look at the presence at any m.room.encryption state event no matter if it's
        // the latest one or if it is well formed
        val realm = realmInstance.getBlockingRealm()
        return EventEntity.whereType(realm, roomId = roomId, type = EventType.STATE_ROOM_ENCRYPTION)
                .query("stateKey == ''")
                .first()
                .find() != null
    }

    /**
     * @param roomId the room Id
     * @param allActive if true return joined as well as invited, if false, only joined
     */
    fun getRoomUserIds(roomId: String, allActive: Boolean): List<String> {
        val realm = realmInstance.getBlockingRealm()
        return if (allActive) {
            RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
        } else {
            RoomMemberHelper(realm, roomId).getJoinedRoomMemberIds()
        }
    }
}
