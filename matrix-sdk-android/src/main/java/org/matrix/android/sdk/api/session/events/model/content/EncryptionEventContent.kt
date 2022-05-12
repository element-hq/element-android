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
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an encrypted event content
 */
@JsonClass(generateAdapter = true)
data class EncryptionEventContent(
        /**
         * Required. The encryption algorithm to be used to encrypt messages sent in this room. Must be 'm.megolm.v1.aes-sha2'.
         */
        @Json(name = "algorithm")
        val algorithm: String?,

        /**
         * How long the session should be used before changing it. 604800000 (a week) is the recommended default.
         */
        @Json(name = "rotation_period_ms")
        val rotationPeriodMs: Long? = null,

        /**
         * How many messages should be sent before changing the session. 100 is the recommended default.
         */
        @Json(name = "rotation_period_msgs")
        val rotationPeriodMsgs: Long? = null
)
