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

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams.Builder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams

/**
 * Create a [RoomSummaryQueryParams] object, calling [init] with a [RoomSummaryQueryParams.Builder].
 */
fun roomSummaryQueryParams(init: (RoomSummaryQueryParams.Builder.() -> Unit) = {}): RoomSummaryQueryParams {
    return RoomSummaryQueryParams.Builder()
            .apply(init)
            .build()
}

/**
 * Create a [SpaceSummaryQueryParams] object (which is a [RoomSummaryQueryParams]), calling [init] with a [RoomSummaryQueryParams.Builder].
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
 * This class can be used to filter room summaries to use with [RoomService].
 * It provides a [Builder].
 * [roomSummaryQueryParams] and [spaceSummaryQueryParams] can also be used to build an instance of this class.
 */
data class RoomSummaryQueryParams(
        /**
         * Query for the roomId.
         */
        val roomId: QueryStringValue,
        /**
         * Query for the displayName of the room. The display name can be the value of the state event,
         * or a value returned by [org.matrix.android.sdk.api.RoomDisplayNameFallbackProvider].
         */
        val displayName: QueryStringValue,
        /**
         * Query for the canonical alias of the room.
         */
        val canonicalAlias: QueryStringValue,
        /**
         * Used to filter room by membership.
         */
        val memberships: List<Membership>,
        /**
         * Used to filter room by room category.
         */
        val roomCategoryFilter: RoomCategoryFilter?,
        /**
         * Used to filter room by room tag.
         */
        val roomTagQueryFilter: RoomTagQueryFilter?,
        /**
         * Used to filter room by room type.
         * @see [includeType]
         */
        val excludeType: List<String?>?,
        /**
         * Used to filter room by room type.
         * @see [excludeType]
         */
        val includeType: List<String?>?,
        /**
         * Used to filter room using the current space.
         */
        val spaceFilter: SpaceFilter,
) {

    /**
     * Builder for [RoomSummaryQueryParams].
     * [roomSummaryQueryParams] and [spaceSummaryQueryParams] can also be used to build an instance of [RoomSummaryQueryParams].
     */
    class Builder {
        var roomId: QueryStringValue = QueryStringValue.NotContains(RoomLocalEcho.PREFIX)
        var displayName: QueryStringValue = QueryStringValue.NoCondition
        var canonicalAlias: QueryStringValue = QueryStringValue.NoCondition
        var memberships: List<Membership> = Membership.all()
        var roomCategoryFilter: RoomCategoryFilter? = null
        var roomTagQueryFilter: RoomTagQueryFilter? = null
        var excludeType: List<String?>? = listOf(RoomType.SPACE)
        var includeType: List<String?>? = null
        var spaceFilter: SpaceFilter = SpaceFilter.NoFilter

        fun build() = RoomSummaryQueryParams(
                roomId = roomId,
                displayName = displayName,
                canonicalAlias = canonicalAlias,
                memberships = memberships,
                roomCategoryFilter = roomCategoryFilter,
                roomTagQueryFilter = roomTagQueryFilter,
                excludeType = excludeType,
                includeType = includeType,
                spaceFilter = spaceFilter,
        )
    }
}
