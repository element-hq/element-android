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

import im.vector.matrix.android.api.session.room.model.RoomMember

internal class RoomMemberDisplayNameResolver {

    fun resolve(userId: String, members: Map<String, RoomMember>): String? {
        val currentMember = members[userId]
        var displayName = currentMember?.displayName
        // Get the user display name from the member list of the room
        // Do not consider null display name

        if (currentMember != null && !currentMember.displayName.isNullOrEmpty()) {
            val hasNameCollision = members
                    .filterValues { it != currentMember && it.displayName == currentMember.displayName }
                    .isNotEmpty()
            if (hasNameCollision) {
                displayName = "${currentMember.displayName} ( $userId )"
            }
        }

        // TODO handle invited users
        /*else if (null != member && TextUtils.equals(member!!.membership, RoomMember.MEMBERSHIP_INVITE)) {
            val user = (mDataHandler as MXDataHandler).getUser(userId)
            if (null != user) {
                displayName = user!!.displayname
            }
        }
        */
        if (displayName == null) {
            // By default, use the user ID
            displayName = userId
        }
        return displayName
    }

}