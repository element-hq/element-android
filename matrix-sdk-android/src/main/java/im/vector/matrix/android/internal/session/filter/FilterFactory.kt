/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.filter

import im.vector.matrix.android.api.session.events.model.EventType

internal object FilterFactory {

    fun createDefaultFilterBody(): FilterBody {
        val filterBody = FilterBody()
        FilterUtil.enableLazyLoading(filterBody, true)
        return filterBody
    }

    fun createRiotFilterBody(): FilterBody {
        val filterBody = FilterBody()
        filterBody.room = RoomFilter().apply {
            timeline = createRiotTimelineFilter()
            state = createRiotStateFilter()
        }
        return filterBody
    }

    fun createDefaultRoomFilter(): RoomEventFilter {
        return RoomEventFilter().apply {
            lazyLoadMembers = true
        }
    }

    fun createRiotRoomFilter(): RoomEventFilter {
        return RoomEventFilter().apply {
            lazyLoadMembers = true
            // TODO Enable this for optimization
            // types = (listOfSupportedEventTypes + listOfSupportedStateEventTypes).toMutableList()
        }
    }

    private fun createRiotTimelineFilter(): RoomEventFilter {
        return RoomEventFilter().apply {
            // TODO Enable this for optimization
            // types = listOfSupportedEventTypes.toMutableList()
        }
    }

    private fun createRiotStateFilter(): RoomEventFilter {
        return RoomEventFilter().apply {
            lazyLoadMembers = true
        }
    }

    // Get only managed types by Riot
    private val listOfSupportedEventTypes = listOf(
            // TODO Complete the list
            EventType.MESSAGE
    )

    // Get only managed types by Riot
    private val listOfSupportedStateEventTypes = listOf(
            // TODO Complete the list
            EventType.STATE_ROOM_MEMBER
    )
}
