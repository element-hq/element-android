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
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.parsing.ForceToBoolean

/**
 * Backup data for one key.
 */
@JsonClass(generateAdapter = true)
internal data class KeyBackupData(
        /**
         * Required. The index of the first message in the session that the key can decrypt.
         */
        @Json(name = "first_message_index")
        val firstMessageIndex: Long,

        /**
         * Required. The number of times this key has been forwarded.
         */
        @Json(name = "forwarded_count")
        val forwardedCount: Int,

        /**
         * Whether the device backing up the key has verified the device that the key is from.
         * Force to boolean because of https://github.com/matrix-org/synapse/issues/6977
         */
        @ForceToBoolean
        @Json(name = "is_verified")
        val isVerified: Boolean,

        /**
         * Algorithm-dependent data.
         */
        @Json(name = "session_data")
        val sessionData: JsonDict,

        /**
         * Flag that indicates whether or not the current inboundSession will be shared to
         * invited users to decrypt past messages.
         */
        @Json(name = "org.matrix.msc3061.shared_history")
        val sharedHistory: Boolean = false
)
