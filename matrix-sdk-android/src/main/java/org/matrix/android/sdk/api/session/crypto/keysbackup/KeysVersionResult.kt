/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
         * algorithm-dependent data, for "m.megolm_backup.v1.curve25519-aes-sha2"
         * see [org.matrix.android.sdk.internal.crypto.keysbackup.MegolmBackupAuthData]
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
