/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.directory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility

@JsonClass(generateAdapter = true)
internal data class RoomDirectoryVisibilityJson(
        /**
         * The visibility of the room in the directory. One of: ["private", "public"]
         */
        @Json(name = "visibility") val visibility: RoomDirectoryVisibility
)
