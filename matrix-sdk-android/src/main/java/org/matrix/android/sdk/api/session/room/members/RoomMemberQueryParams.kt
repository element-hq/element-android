/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.members

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.room.model.Membership

fun roomMemberQueryParams(init: (RoomMemberQueryParams.Builder.() -> Unit) = {}): RoomMemberQueryParams {
    return RoomMemberQueryParams.Builder().apply(init).build()
}

/**
 * This class can be used to filter room members.
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
