/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class RoomHistoryVisibilityContent(
        @Json(name = "history_visibility") val historyVisibilityStr: String? = null
) {
    val historyVisibility: RoomHistoryVisibility? = RoomHistoryVisibility.values()
            .find { it.value == historyVisibilityStr }
            ?: run {
                Timber.w("Invalid value for RoomHistoryVisibility: `$historyVisibilityStr`")
                null
            }
}
