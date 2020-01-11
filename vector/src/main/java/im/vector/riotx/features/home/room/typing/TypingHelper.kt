/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.riotx.features.home.room.typing

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class TypingHelper @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider
) {
    /**
     * Exclude current user from the list of typing users
     */
    fun excludeCurrentUser(
            typingUserIds: List<String>
    ): List<String> {
        return typingUserIds
                .filter { it != session.myUserId }
    }

    /**
     * Convert a list of userId to a list of maximum 3 UserItems
     */
    fun toTypingRoomMembers(
            typingUserIds: List<String>,
            membershipService: MembershipService?
    ): List<MatrixItem.UserItem> {
        return excludeCurrentUser(typingUserIds)
                .take(3)
                .mapNotNull { membershipService?.getRoomMember(it) }
                .map { it.toMatrixItem() }
    }

    /**
     * Convert a list of typing UserItems to a human readable String
     */
    fun toTypingMessage(typingUserItems: List<MatrixItem.UserItem>): String? {
        return when {
            typingUserItems.isEmpty() ->
                null
            typingUserItems.size == 1 ->
                stringProvider.getString(R.string.room_one_user_is_typing, typingUserItems[0].getBestName())
            typingUserItems.size == 2 ->
                stringProvider.getString(R.string.room_two_users_are_typing, typingUserItems[0].getBestName(), typingUserItems[1].getBestName())
            else                      ->
                stringProvider.getString(R.string.room_many_users_are_typing, typingUserItems[0].getBestName(), typingUserItems[1].getBestName())
        }
    }
}
