/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PollType {
    /**
     * Voters should see results as soon as they have voted.
     */
    @Json(name = "org.matrix.msc3381.poll.disclosed")
    DISCLOSED_UNSTABLE,

    @Json(name = "m.poll.disclosed")
    DISCLOSED,

    /**
     * Results should be only revealed when the poll is ended.
     */
    @Json(name = "org.matrix.msc3381.poll.undisclosed")
    UNDISCLOSED_UNSTABLE,

    @Json(name = "m.poll.undisclosed")
    UNDISCLOSED
}
