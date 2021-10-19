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

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import javax.inject.Inject

internal class RoomMemberEventHandler @Inject constructor(
        @UserId private val myUserId: String
) {

    fun handle(realm: Realm, roomId: String, event: Event, aggregator: SyncResponsePostTreatmentAggregator? = null): Boolean {
        if (event.type != EventType.STATE_ROOM_MEMBER) {
            return false
        }
        val userId = event.stateKey ?: return false
        val roomMember = event.getFixedRoomMemberContent()
        return handle(realm, roomId, userId, roomMember, aggregator)
    }

    fun handle(realm: Realm,
               roomId: String,
               userId: String,
               roomMember: RoomMemberContent?,
               aggregator: SyncResponsePostTreatmentAggregator? = null): Boolean {
        if (roomMember == null) {
            return false
        }
        val roomMemberEntity = RoomMemberEntityFactory.create(
                roomId,
                userId,
                roomMember,
                // When an update is happening, insertOrUpdate replace existing values with null if they are not provided,
                // but we want to preserve presence record value and not replace it with null
                getExistingPresenceState(realm, roomId, userId))
        realm.insertOrUpdate(roomMemberEntity)
        if (roomMember.membership.isActive()) {
            val userEntity = UserEntityFactory.create(userId, roomMember)
            realm.insertOrUpdate(userEntity)
        }

        // check whether this new room member event may be used to update the directs dictionary in account data
        // this is required to handle correctly invite by email in DM
        val mxId = roomMember.thirdPartyInvite?.signed?.mxid
        if (mxId != null && mxId != myUserId) {
            aggregator?.directChatsToCheck?.put(roomId, mxId)
        }
        return true
    }

    /**
     * Get the already existing presence state for a specific user & room in order NOT to be replaced in RoomMemberSummaryEntity
     * by NULL value.
     */

    private fun getExistingPresenceState(realm: Realm, roomId: String, userId: String): UserPresenceEntity? {
        return RoomMemberSummaryEntity.where(realm, roomId, userId).findFirst()?.userPresenceEntity
    }
}
