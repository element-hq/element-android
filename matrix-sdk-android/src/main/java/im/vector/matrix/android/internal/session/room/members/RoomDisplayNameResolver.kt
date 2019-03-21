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

package im.vector.matrix.android.internal.session.room.members

import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.R
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomAliasesContent
import im.vector.matrix.android.api.session.room.model.RoomCanonicalAliasContent
import im.vector.matrix.android.api.session.room.model.RoomNameContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where

/**
 * This class computes room display name
 */
internal class RoomDisplayNameResolver(private val monarchy: Monarchy,
                                       private val roomMemberDisplayNameResolver: RoomMemberDisplayNameResolver,
                                       private val credentials: Credentials
) {

    /**
     * Compute the room display name
     *
     * @param context
     * @param roomId: the roomId to resolve the name of.
     * @return the room display name
     */
    fun resolve(context: Context, roomId: String): CharSequence {
        // this algorithm is the one defined in
        // https://github.com/matrix-org/matrix-js-sdk/blob/develop/lib/models/room.js#L617
        // calculateRoomName(room, userId)

        // For Lazy Loaded room, see algorithm here:
        // https://docs.google.com/document/d/11i14UI1cUz-OJ0knD5BFu7fmT6Fo327zvMYqfSAR7xs/edit#heading=h.qif6pkqyjgzn
        var name: CharSequence? = null
        monarchy.doWithRealm { realm ->
            val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()
            val roomName = EventEntity.where(realm, roomId, EventType.STATE_ROOM_NAME).prev()?.asDomain()
            name = roomName?.content.toModel<RoomNameContent>()?.name
            if (!name.isNullOrEmpty()) {
                return@doWithRealm
            }

            val canonicalAlias = EventEntity.where(realm, roomId, EventType.STATE_CANONICAL_ALIAS).prev()?.asDomain()
            name = canonicalAlias?.content.toModel<RoomCanonicalAliasContent>()?.canonicalAlias
            if (!name.isNullOrEmpty()) {
                return@doWithRealm
            }

            val aliases = EventEntity.where(realm, roomId, EventType.STATE_ROOM_ALIASES).prev()?.asDomain()
            name = aliases?.content.toModel<RoomAliasesContent>()?.aliases?.firstOrNull()
            if (!name.isNullOrEmpty()) {
                return@doWithRealm
            }

            val roomMembers = RoomMembers(realm, roomId)
            val otherRoomMembers = roomMembers.getLoaded()
                    .filterKeys { it != credentials.userId }

            if (roomEntity?.membership == MyMembership.INVITED) {
                //TODO handle invited
                /*
                if (currentUser != null
                    && !othersActiveMembers.isEmpty()
                    && !TextUtils.isEmpty(currentUser!!.mSender)) {
                    // extract who invited us to the room
                    name = context.getString(R.string.room_displayname_invite_from, roomState.resolve(currentUser!!.mSender))
                } else {
                    name = context.getString(R.string.room_displayname_room_invite)
                }
                */
                name = context.getString(R.string.room_displayname_room_invite)
            } else {

                val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                val memberIds = if (roomSummary?.heroes?.isNotEmpty() == true) {
                    roomSummary.heroes
                } else {
                    otherRoomMembers.keys.toList()
                }

                val nbOfOtherMembers = memberIds.size

                when (nbOfOtherMembers) {
                    0    -> name = context.getString(R.string.room_displayname_empty_room)
                    1    -> name = roomMemberDisplayNameResolver.resolve(memberIds[0], otherRoomMembers)
                    2    -> {
                        val member1 = memberIds[0]
                        val member2 = memberIds[1]
                        name = context.getString(R.string.room_displayname_two_members,
                                roomMemberDisplayNameResolver.resolve(member1, otherRoomMembers),
                                roomMemberDisplayNameResolver.resolve(member2, otherRoomMembers)
                        )
                    }
                    else -> {
                        val member = memberIds[0]
                        name = context.resources.getQuantityString(R.plurals.room_displayname_three_and_more_members,
                                roomMembers.getNumberOfJoinedMembers() - 1,
                                roomMemberDisplayNameResolver.resolve(member, otherRoomMembers),
                                roomMembers.getNumberOfJoinedMembers() - 1)
                    }
                }
            }
            return@doWithRealm
        }
        return name ?: roomId
    }
}
