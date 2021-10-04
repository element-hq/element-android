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

package org.matrix.android.sdk.api.session.room.members

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.room.model.Membership

fun roomMemberQueryParams(init: (RoomMemberQueryParams.Builder.() -> Unit) = {}): RoomMemberQueryParams {
    return RoomMemberQueryParams.Builder().apply(init).build()
}

/**
 * This class can be used to filter room members
 */
data class RoomMemberQueryParams(
        val displayName: QueryStringValue,
        val memberships: List<Membership>,
        val userId: QueryStringValue,
        val excludeSelf: Boolean
) {

    class Builder {

        var userId: QueryStringValue = QueryStringValue.NoCondition
        var displayName: QueryStringValue = QueryStringValue.IsNotEmpty
        var memberships: List<Membership> = Membership.all()
        var excludeSelf: Boolean = false

        fun build() = RoomMemberQueryParams(
                displayName = displayName,
                memberships = memberships,
                userId = userId,
                excludeSelf = excludeSelf
        )
    }
}
