/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.room.model.RoomSummary

interface UpdatableLivePageResult {
    val livePagedList: LiveData<PagedList<RoomSummary>>
    val liveBoundaries: LiveData<ResultBoundaries>
    var queryParams: RoomSummaryQueryParams
}

data class ResultBoundaries(
        val frontLoaded: Boolean = false,
        val endLoaded: Boolean = false,
        val zeroItemLoaded: Boolean = false
)
