/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
