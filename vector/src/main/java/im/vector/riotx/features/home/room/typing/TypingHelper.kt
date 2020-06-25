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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class TypingHelper @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider
) {

    /**
     * Return true if some users are currently typing in the room (excluding yourself).
     */
    fun hasTypingUsers(roomId: String): LiveData<Boolean> {
        val liveData = session.typingUsersTracker().getTypingUsersLive(roomId)
        return Transformations.map(liveData) {
            it.isNotEmpty()
        }
    }

    /**
     * Returns a human readable String of currently typing users in the room (excluding yourself).
     */
    fun getTypingMessage(roomId: String): LiveData<String> {
        val liveData = session.typingUsersTracker().getTypingUsersLive(roomId)
        return Transformations.map(liveData) { typingUsers ->
            when {
                typingUsers.isEmpty() ->
                    ""
                typingUsers.size == 1 ->
                    stringProvider.getString(R.string.room_one_user_is_typing, typingUsers[0].disambiguatedDisplayName)
                typingUsers.size == 2 ->
                    stringProvider.getString(R.string.room_two_users_are_typing,
                            typingUsers[0].disambiguatedDisplayName,
                            typingUsers[1].disambiguatedDisplayName)
                else                  ->
                    stringProvider.getString(R.string.room_many_users_are_typing,
                            typingUsers[0].disambiguatedDisplayName,
                            typingUsers[1].disambiguatedDisplayName)
            }
        }
    }
}
