/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.sync.filter

import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.sync.filter.SyncFilterParams
import org.matrix.android.sdk.internal.session.filter.Filter
import org.matrix.android.sdk.internal.session.filter.RoomEventFilter
import org.matrix.android.sdk.internal.session.filter.RoomFilter

internal class SyncFilterBuilder {
    private var lazyLoadMembersForStateEvents: Boolean? = null
    private var lazyLoadMembersForMessageEvents: Boolean? = null
    private var useThreadNotifications: Boolean? = null
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

    internal fun with(currentFilterParams: SyncFilterParams?) =
            apply {
                currentFilterParams?.let {
                    useThreadNotifications = currentFilterParams.useThreadNotifications
                    lazyLoadMembersForMessageEvents = currentFilterParams.lazyLoadMembersForMessageEvents
                    lazyLoadMembersForStateEvents = currentFilterParams.lazyLoadMembersForStateEvents
                    listOfSupportedEventTypes = currentFilterParams.listOfSupportedEventTypes?.toList()
                    listOfSupportedStateEventTypes = currentFilterParams.listOfSupportedStateEventTypes?.toList()
                }
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

    private fun buildTimelineFilter(homeServerCapabilities: HomeServerCapabilities): RoomEventFilter? {
        val resolvedUseThreadNotifications = if (homeServerCapabilities.canUseThreadReadReceiptsAndNotifications) {
            useThreadNotifications
        } else {
            null
        }
        return RoomEventFilter(
                enableUnreadThreadNotifications = resolvedUseThreadNotifications,
                lazyLoadMembers = lazyLoadMembersForMessageEvents
        ).orNullIfEmpty()
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SyncFilterBuilder

        if (lazyLoadMembersForStateEvents != other.lazyLoadMembersForStateEvents) return false
        if (lazyLoadMembersForMessageEvents != other.lazyLoadMembersForMessageEvents) return false
        if (useThreadNotifications != other.useThreadNotifications) return false
        if (listOfSupportedEventTypes != other.listOfSupportedEventTypes) return false
        if (listOfSupportedStateEventTypes != other.listOfSupportedStateEventTypes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lazyLoadMembersForStateEvents?.hashCode() ?: 0
        result = 31 * result + (lazyLoadMembersForMessageEvents?.hashCode() ?: 0)
        result = 31 * result + (useThreadNotifications?.hashCode() ?: 0)
        result = 31 * result + (listOfSupportedEventTypes?.hashCode() ?: 0)
        result = 31 * result + (listOfSupportedStateEventTypes?.hashCode() ?: 0)
        return result
    }
}
