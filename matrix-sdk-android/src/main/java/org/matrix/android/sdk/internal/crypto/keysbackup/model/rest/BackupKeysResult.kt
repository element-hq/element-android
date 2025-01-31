/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BackupKeysResult(
        // The hash value which is an opaque string representing stored keys in the backup
        @Json(name = "etag")
        val hash: String,

        // The number of keys stored in the backup.
        @Json(name = "count")
        val count: Int
)
