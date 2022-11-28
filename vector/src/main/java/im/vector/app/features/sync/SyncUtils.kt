/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.sync

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.sync.filter.SyncFilterBuilder

object SyncUtils {
    // Get only managed types by Element
    private val listOfSupportedTimelineEventTypes = listOf(
            // TODO Complete the list
            EventType.MESSAGE
    )

    // Get only managed types by Element
    private val listOfSupportedStateEventTypes = listOf(
            // TODO Complete the list
            EventType.STATE_ROOM_MEMBER
    )

    fun getSyncFilterBuilder(): SyncFilterBuilder {
        return SyncFilterBuilder()
                .useThreadNotifications(true)
                .lazyLoadMembersForStateEvents(true)
        /**
         * Currently we don't set [lazy_load_members = true] for Filter.room.timeline even though we set it for RoomFilter which is used later to
         * fetch messages in a room. It's not clear if it's done so by mistake or intentionally, so changing it could case side effects and need
         * careful testing
         * */
//                .lazyLoadMembersForMessageEvents(true)
//                .listOfSupportedStateEventTypes(listOfSupportedStateEventTypes)
//                .listOfSupportedTimelineEventTypes(listOfSupportedTimelineEventTypes)
    }
}
