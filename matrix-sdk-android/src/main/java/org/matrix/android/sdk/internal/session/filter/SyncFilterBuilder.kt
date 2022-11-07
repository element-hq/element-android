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

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.internal.database.model.SyncFilterParamsEntity

class SyncFilterBuilder {
    private var lazyLoadMembersForStateEvents: Boolean? = true
    private var lazyLoadMembersForMessageEvents: Boolean? = true
    private var useThreadNotifications: Boolean? = false
    private var listOfSupportedEventTypes: List<String>? = null
    private var listOfSupportedStateEventTypes: List<String>? = null

    fun lazyLoadMembersForStateEvents(lazyLoadMembersForStateEvents: Boolean) = apply { this.lazyLoadMembersForStateEvents = lazyLoadMembersForStateEvents }

    fun lazyLoadMembersForMessageEvents(lazyLoadMembersForMessageEvents: Boolean) =
            apply { this.lazyLoadMembersForMessageEvents = lazyLoadMembersForMessageEvents }

    fun useThreadNotifications(useThreadNotifications: Boolean) =
            apply { this.useThreadNotifications = useThreadNotifications }

    fun listOfSupportedStateEventTypes(listOfSupportedStateEventTypes: List<String>) =
            apply { this.listOfSupportedStateEventTypes = listOfSupportedStateEventTypes }

    fun listOfSupportedTimelineEventTypes(listOfSupportedEventTypes: List<String>) =
            apply { this.listOfSupportedEventTypes = listOfSupportedEventTypes }

    internal fun with(currentFilterParams: SyncFilterParamsEntity?) =
            apply {
                currentFilterParams?.let {
                    useThreadNotifications = currentFilterParams.useThreadNotifications
                    lazyLoadMembersForMessageEvents = currentFilterParams.lazyLoadMembersForMessageEvents
                    lazyLoadMembersForStateEvents = currentFilterParams.lazyLoadMembersForStateEvents
                }
            }

    internal fun extractParams(): SyncFilterParamsEntity {
        return SyncFilterParamsEntity(
                useThreadNotifications = useThreadNotifications,
                lazyLoadMembersForMessageEvents = lazyLoadMembersForMessageEvents,
                lazyLoadMembersForStateEvents = lazyLoadMembersForStateEvents,
        )
    }

    internal fun build(homeServerCapabilities: HomeServerCapabilities): Filter {
        return Filter(
                room = buildRoomFilter(homeServerCapabilities)
        )
    }

    private fun buildRoomFilter(homeServerCapabilities: HomeServerCapabilities): RoomFilter {
        return RoomFilter(
                timeline = buildTimelineFilter(homeServerCapabilities),
                state = buildStateFilter()
        )
    }

    private fun buildTimelineFilter(homeServerCapabilities: HomeServerCapabilities): RoomEventFilter? =
            RoomEventFilter(
                    enableUnreadThreadNotifications = useThreadNotifications == true && homeServerCapabilities.canUseThreadReadReceiptsAndNotifications,
                    lazyLoadMembers = lazyLoadMembersForMessageEvents
            ).orNullIfEmpty()

    private fun buildStateFilter(): RoomEventFilter? =
            RoomEventFilter(
                    lazyLoadMembers = lazyLoadMembersForStateEvents,
                    types = listOfSupportedStateEventTypes
            ).orNullIfEmpty()

    private fun RoomEventFilter.orNullIfEmpty(): RoomEventFilter? {
        return if (hasData()) {
            this
        } else {
            null
        }
    }
}
