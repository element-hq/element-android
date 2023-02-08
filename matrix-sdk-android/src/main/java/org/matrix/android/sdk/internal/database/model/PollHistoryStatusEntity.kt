/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.internal.session.room.poll.PollConstants

/**
 * Keeps track of the loading process of the poll history.
 */
internal open class PollHistoryStatusEntity(
        /**
         * The related room id.
         */
        @PrimaryKey
        var roomId: String = "",

        /**
         * Timestamp of the in progress poll sync target in backward direction in milliseconds.
         */
        var currentTimestampTargetBackwardMs: Long? = null,

        /**
         * Timestamp of the oldest event synced once target has been reached in milliseconds.
         */
        var oldestTimestampTargetReachedMs: Long? = null,

        /**
         * Id of the oldest event synced.
         */
        var oldestEventIdReached: String? = null,

        /**
         * Id of the most recent event synced.
         */
        var mostRecentEventIdReached: String? = null,

        /**
         * Indicate whether all polls in a room have been synced in backward direction.
         */
        var isEndOfPollsBackward: Boolean = false,
) : RealmObject() {

    companion object

    /**
     * Create a new instance of the entity with the same content.
     */
    fun copy(): PollHistoryStatusEntity {
        return PollHistoryStatusEntity(
                roomId = roomId,
                currentTimestampTargetBackwardMs = currentTimestampTargetBackwardMs,
                oldestTimestampTargetReachedMs = oldestTimestampTargetReachedMs,
                oldestEventIdReached = oldestEventIdReached,
                mostRecentEventIdReached = mostRecentEventIdReached,
                isEndOfPollsBackward = isEndOfPollsBackward,
        )
    }

    /**
     * Indicate whether at least one poll sync has been fully completed backward for the given room.
     */
    val hasCompletedASyncBackward: Boolean
        get() = oldestTimestampTargetReachedMs != null

    /**
     * Indicate whether all polls in a room have been synced for the current timestamp target in backward direction.
     */
    val currentTimestampTargetBackwardReached: Boolean
        get() = checkIfCurrentTimestampTargetBackwardIsReached()

    private fun checkIfCurrentTimestampTargetBackwardIsReached(): Boolean {
        val currentTarget = currentTimestampTargetBackwardMs
        val lastTarget = oldestTimestampTargetReachedMs
        // last timestamp target should be older or equal to the current target
        return currentTarget != null && lastTarget != null && lastTarget <= currentTarget
    }

    /**
     * Compute the number of days of history currently synced.
     */
    fun getNbSyncedDays(currentMs: Long): Int {
        val oldestTimestamp = oldestTimestampTargetReachedMs
        return if (oldestTimestamp == null) {
            0
        } else {
            ((currentMs - oldestTimestamp).coerceAtLeast(0) / PollConstants.MILLISECONDS_PER_DAY).toInt()
        }
    }
}
