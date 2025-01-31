/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules

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

        val numMembers = room.membershipService().getNumberOfJoinedMembers()

        return when (prefix) {
            "<" -> numMembers < count
            ">" -> numMembers > count
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
