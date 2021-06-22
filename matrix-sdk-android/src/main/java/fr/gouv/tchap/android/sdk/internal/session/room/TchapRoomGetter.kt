/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.android.sdk.internal.session.room

import android.util.Patterns
import org.matrix.android.sdk.api.session.events.model.EventType.STATE_ROOM_CREATE
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.room.DefaultRoomGetter
import org.matrix.android.sdk.internal.session.room.RoomFactory
import javax.inject.Inject

@SessionScope
internal class TchapRoomGetter @Inject constructor(
        private val realmSessionProvider: RealmSessionProvider,
        private val roomFactory: RoomFactory
) : DefaultRoomGetter(realmSessionProvider, roomFactory) {

    override fun getDirectRoomWith(otherUserId: String): String? {
        val directRoomMemberships = realmSessionProvider.withRealm { realm ->
            // get direct room entities matching the given user id
            RoomSummaryEntity
                    .where(realm)
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                    .equalTo(RoomSummaryEntityFields.DIRECT_USER_ID, otherUserId)
                    .findAll()
        }
                // convert to Room objects
                .map { roomFactory.create(it.roomId) }
                // sort from the oldest to the most recent
                .sortedBy { it.getStateEvent(STATE_ROOM_CREATE)?.originServerTs }
                // convert to DirectRoomMemberships objects
                .map { DirectRoomMemberships(it.roomId, it.roomSummary()?.membership, it.getRoomMember(otherUserId)?.membership) }

        // In the description of the memberships, we display first the current user status and the other member in second.
        // We review all the direct chats by considering the memberships in the following priorities:
        // 1. join-join
        // 2. invite-join
        // 3. join-invite
        // 4. join-left (or invite-left)
        // The case left-x isn't possible because we ignore for the moment the left rooms.
        // If other member user id is an email, we take the oldest room.

        return directRoomMemberships.firstOrNull { it.first == Membership.JOIN && it.second == Membership.JOIN }?.roomId // join - join
                ?: directRoomMemberships.firstOrNull { it.first == Membership.INVITE && it.second == Membership.JOIN }?.roomId // invite - join
                ?: directRoomMemberships.firstOrNull { it.first == Membership.JOIN && it.second == Membership.INVITE }?.roomId // join - invite
                ?: directRoomMemberships.firstOrNull { it.first?.isActive() == true && it.second == Membership.LEAVE }?.roomId // join or invite - left
                ?: directRoomMemberships // otherUserId is an email
                        .takeIf { Patterns.EMAIL_ADDRESS.matcher(otherUserId).matches() }
                        ?.firstOrNull { it.first == Membership.JOIN }
                        ?.roomId
    }

    /**
     * @property roomId the room identifier
     * @property first the first user membership
     * @property second the second user membership
     */
    private data class DirectRoomMemberships(
            val roomId: String,
            val first: Membership?,
            val second: Membership?
    )
}
