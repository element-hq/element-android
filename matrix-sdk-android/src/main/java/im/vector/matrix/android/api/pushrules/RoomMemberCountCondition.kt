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
package im.vector.matrix.android.api.pushrules

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.RoomService
import timber.log.Timber
import java.util.regex.Pattern

private val regex = Pattern.compile("^(==|<=|>=|<|>)?(\\d*)$")

class RoomMemberCountCondition(val `is`: String) : Condition(Kind.room_member_count) {

    override fun isSatisfied(conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveRoomMemberCountCondition(this)
    }

    override fun technicalDescription(): String {
        return "Room member count is $`is`"
    }

    fun isSatisfied(event: Event, session: RoomService?): Boolean {
        // sanity check^
        val roomId = event.roomId ?: return false
        val room = session?.getRoom(roomId) ?: return false

        // Parse the is field into prefix and number the first time
        val (prefix, count) = parseIsField() ?: return false

        val numMembers = room.getNumberOfJoinedMembers()

        return when (prefix) {
            "<"  -> numMembers < count
            ">"  -> numMembers > count
            "<=" -> numMembers <= count
            ">=" -> numMembers >= count
            else -> numMembers == count
        }
    }

    /**
     * Parse the is field to extract meaningful information.
     */
    private fun parseIsField(): Pair<String?, Int>? {
        try {
            val match = regex.matcher(`is`)
            if (match.find()) {
                val prefix = match.group(1)
                val count = match.group(2).toInt()
                return prefix to count
            }
        } catch (t: Throwable) {
            Timber.d(t)
        }
        return null

    }
}