/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PollCreationInfo(
        @Json(name = "question") val question: PollQuestion? = null,
        @Json(name = "kind") val kind: PollType? = PollType.DISCLOSED_UNSTABLE,
        @Json(name = "max_selections") val maxSelections: Int = 1,
        @Json(name = "answers") val answers: List<PollAnswer>? = null
) {

    fun isUndisclosed() = kind in listOf(PollType.UNDISCLOSED_UNSTABLE, PollType.UNDISCLOSED)
}
