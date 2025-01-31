/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.query

import io.realm.RealmQuery
import io.realm.Sort
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields

internal fun RealmQuery<RoomSummaryEntity>.process(sortOrder: RoomSortOrder): RealmQuery<RoomSummaryEntity> {
    when (sortOrder) {
        RoomSortOrder.NAME -> {
            sort(RoomSummaryEntityFields.DISPLAY_NAME, Sort.ASCENDING)
        }
        RoomSortOrder.ACTIVITY -> {
            sort(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Sort.DESCENDING)
        }
        RoomSortOrder.PRIORITY_AND_ACTIVITY -> {
            sort(
                    arrayOf(
                            RoomSummaryEntityFields.IS_FAVOURITE,
                            RoomSummaryEntityFields.IS_LOW_PRIORITY,
                            RoomSummaryEntityFields.LAST_ACTIVITY_TIME
                    ),
                    arrayOf(Sort.DESCENDING, Sort.ASCENDING, Sort.DESCENDING)
            )
        }
        RoomSortOrder.NONE -> {
        }
    }
    return this
}
