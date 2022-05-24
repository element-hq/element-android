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

package org.matrix.android.sdk.api.session.room

import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams

/**
 * Create a [RoomSummaryQueryParams] object, calling [init] with a [RoomSummaryQueryParams.Builder]
 */
fun roomSummaryQueryParams(init: (RoomSummaryQueryParams.Builder.() -> Unit) = {}): RoomSummaryQueryParams {
    return RoomSummaryQueryParams.Builder()
            .apply(init)
            .build()
}

/**
 * Create a [SpaceSummaryQueryParams] object (which is a [RoomSummaryQueryParams]), calling [init] with a [RoomSummaryQueryParams.Builder]
 * This is specific for spaces, other filters will be applied after invoking [init]
 */
fun spaceSummaryQueryParams(init: (RoomSummaryQueryParams.Builder.() -> Unit) = {}): SpaceSummaryQueryParams {
    return roomSummaryQueryParams {
        init()
        includeType = listOf(RoomType.SPACE)
        excludeType = null
        roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
    }
}

/**
 * This class can be used to filter room summaries to use with:
 * [org.matrix.android.sdk.api.session.room.Room] and [org.matrix.android.sdk.api.session.room.RoomService].
 */
data class RoomSummaryQueryParams(
        val displayName: QueryStringValue,
        val canonicalAlias: QueryStringValue,
        val memberships: List<Membership>,
        val roomCategoryFilter: RoomCategoryFilter?,
        val roomTagQueryFilter: RoomTagQueryFilter?,
        val excludeType: List<String?>?,
        val includeType: List<String?>?,
        val activeSpaceFilter: ActiveSpaceFilter?,
        val activeGroupId: String? = null
) {

    class Builder {
        var displayName: QueryStringValue = QueryStringValue.IsNotEmpty
        var canonicalAlias: QueryStringValue = QueryStringValue.NoCondition
        var memberships: List<Membership> = Membership.all()
        var roomCategoryFilter: RoomCategoryFilter? = RoomCategoryFilter.ALL
        var roomTagQueryFilter: RoomTagQueryFilter? = null
        var excludeType: List<String?>? = listOf(RoomType.SPACE)
        var includeType: List<String?>? = null
        var activeSpaceFilter: ActiveSpaceFilter = ActiveSpaceFilter.None
        var activeGroupId: String? = null

        fun build() = RoomSummaryQueryParams(
                displayName = displayName,
                canonicalAlias = canonicalAlias,
                memberships = memberships,
                roomCategoryFilter = roomCategoryFilter,
                roomTagQueryFilter = roomTagQueryFilter,
                excludeType = excludeType,
                includeType = includeType,
                activeSpaceFilter = activeSpaceFilter,
                activeGroupId = activeGroupId
        )
    }
}
