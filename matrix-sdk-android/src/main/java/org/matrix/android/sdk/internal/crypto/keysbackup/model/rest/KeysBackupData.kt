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
 * Backup data for several keys in several rooms.
 */
@JsonClass(generateAdapter = true)
internal data class KeysBackupData(
        // the keys are the room IDs, and the values are RoomKeysBackupData
        @Json(name = "rooms")
        val roomIdToRoomKeysBackupData: MutableMap<String, RoomKeysBackupData> = HashMap()
)
