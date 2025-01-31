/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keysbackup

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysAlgorithmAndData

@JsonClass(generateAdapter = true)
data class KeysVersionResult(
        /**
         * The algorithm used for storing backups. Currently, only "m.megolm_backup.v1.curve25519-aes-sha2" is defined
         */
        @Json(name = "algorithm")
        override val algorithm: String,

        /**
         * algorithm-dependent data, for "m.megolm_backup.v1.curve25519-aes-sha2".
         * @see [org.matrix.android.sdk.internal.crypto.keysbackup.MegolmBackupAuthData]
         */
        @Json(name = "auth_data")
        override val authData: JsonDict,

        // the backup version
        @Json(name = "version")
        val version: String,

        // The hash value which is an opaque string representing stored keys in the backup
        @Json(name = "etag")
        val hash: String,

        // The number of keys stored in the backup.
        @Json(name = "count")
        val count: Int
) : KeysAlgorithmAndData
