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

/**
 * Backup data for several keys within a room.
 */
@JsonClass(generateAdapter = true)
internal data class RoomKeysBackupData(
        // the keys are the session IDs, and the values are KeyBackupData
        @Json(name = "sessions")
        val sessionIdToKeyBackupData: MutableMap<String, KeyBackupData> = HashMap()
)
