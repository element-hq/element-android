/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.model.accountdata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.emptyJsonDict

@JsonClass(generateAdapter = true)
internal data class IgnoredUsersContent(
        /**
         * Required. The map of users to ignore. UserId -> empty object for future enhancement
         */
        @Json(name = "ignored_users") val ignoredUsers: Map<String, Any>
) {

    companion object {
        fun createWithUserIds(userIds: List<String>): IgnoredUsersContent {
            return IgnoredUsersContent(
                    ignoredUsers = userIds.associateWith { emptyJsonDict }
            )
        }
    }
}
