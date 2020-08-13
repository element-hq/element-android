/*
 * Copyright 2018 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.keysbackup.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.ForceToBoolean

/**
 * Backup data for one key.
 */
@JsonClass(generateAdapter = true)
data class KeyBackupData(
        /**
         * Required. The index of the first message in the session that the key can decrypt.
         */
        @Json(name = "first_message_index")
        val firstMessageIndex: Long = 0,

        /**
         * Required. The number of times this key has been forwarded.
         */
        @Json(name = "forwarded_count")
        val forwardedCount: Int = 0,

        /**
         * Whether the device backing up the key has verified the device that the key is from.
         * Force to boolean because of https://github.com/matrix-org/synapse/issues/6977
         */
        @ForceToBoolean
        @Json(name = "is_verified")
        val isVerified: Boolean = false,

        /**
         * Algorithm-dependent data.
         */
        @Json(name = "session_data")
        val sessionData: Map<String, Any>? = null
) {

    fun toJsonString(): String {
        return MoshiProvider.providesMoshi().adapter(KeyBackupData::class.java).toJson(this)
    }
}
