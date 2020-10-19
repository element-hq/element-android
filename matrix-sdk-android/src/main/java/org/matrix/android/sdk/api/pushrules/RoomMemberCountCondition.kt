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
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.room.RoomGetter
import timber.log.Timber

private val regex = Regex("^(==|<=|>=|<|>)?(\\d*)$")

class RoomMemberCountCondition(
        /**
         * A decimal integer optionally prefixed by one of ==, <, >, >= or <=.
         * A prefix of < matches rooms where the member count is strictly less than the given number and so forth.
         * If no prefix is present, this parameter defaults to ==.
         */
        val iz: String
) : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveRoomMemberCountCondition(event, this)
    }

    override fun technicalDescription() = "Room member count is $iz"

    internal fun isSatisfied(event: Event, roomGetter: RoomGetter): Boolean {
        // sanity checks
        val roomId = event.roomId ?: return false
        val room = roomGetter.getRoom(roomId) ?: return false

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
            val match = regex.find(iz) ?: return null
            val (prefix, count) = match.destructured
            return prefix to count.toInt()
        } catch (t: Throwable) {
            Timber.e(t, "Unable to parse 'is' field")
        }
        return null
    }
}
