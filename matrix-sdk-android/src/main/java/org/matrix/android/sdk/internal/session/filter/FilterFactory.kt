/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import timber.log.Timber

internal object FilterFactory {

    fun createThreadsFilter(numberOfEvents: Int, userId: String?): RoomEventFilter {
        Timber.i("$userId")
        return RoomEventFilter(
                limit = numberOfEvents,
//                senders = listOf(userId),
//                relationSenders = userId?.let { listOf(it) },
                relationTypes = listOf(RelationType.THREAD)
        )
    }

    fun createUploadsFilter(numberOfEvents: Int): RoomEventFilter {
        return RoomEventFilter(
                limit = numberOfEvents,
                containsUrl = true,
                types = listOf(EventType.MESSAGE),
                lazyLoadMembers = true
        )
    }

    fun createDefaultFilter(): Filter {
        return FilterUtil.enableLazyLoading(Filter(), true)
    }

    fun createElementFilter(): Filter {
        return Filter(
                room = RoomFilter(
                        timeline = createElementTimelineFilter(),
                        state = createElementStateFilter()
                )
        )
    }

    fun createDefaultRoomFilter(): RoomEventFilter {
        return RoomEventFilter(
                lazyLoadMembers = true
        )
    }

    fun createElementRoomFilter(): RoomEventFilter {
        return RoomEventFilter(
                lazyLoadMembers = true
                // TODO Enable this for optimization
                // types = (listOfSupportedEventTypes + listOfSupportedStateEventTypes).toMutableList()
        )
    }

    private fun createElementTimelineFilter(): RoomEventFilter? {
        return null // RoomEventFilter().apply {
        // TODO Enable this for optimization
        // types = listOfSupportedEventTypes.toMutableList()
        // }
    }

    private fun createElementStateFilter(): RoomEventFilter {
        return RoomEventFilter(
                lazyLoadMembers = true
        )
    }

    // Get only managed types by Element
    private val listOfSupportedEventTypes = listOf(
            // TODO Complete the list
            EventType.MESSAGE
    )

    // Get only managed types by Element
    private val listOfSupportedStateEventTypes = listOf(
            // TODO Complete the list
            EventType.STATE_ROOM_MEMBER
    )
}
