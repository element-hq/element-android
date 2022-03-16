/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.crypto.model.event.WithHeldCode

enum class TrailType {
    OutgoingKeyForward,
    IncomingKeyForward,
    OutgoingKeyWithheld,
    IncomingKeyRequest,
    Unknown
}

interface AuditInfo {
    val roomId: String
    val sessionId: String
    val senderKey: String
    val alg: String
    val userId: String
    val deviceId: String
}

@JsonClass(generateAdapter = true)
data class ForwardInfo(
        override val roomId: String,
        override val sessionId: String,
        override val senderKey: String,
        override val alg: String,
        override val userId: String,
        override val deviceId: String,
        val chainIndex: Long?
) : AuditInfo

@JsonClass(generateAdapter = true)
data class WithheldInfo(
        override val roomId: String,
        override val sessionId: String,
        override val senderKey: String,
        override val alg: String,
        val code: WithHeldCode,
        override val userId: String,
        override val deviceId: String
) : AuditInfo

@JsonClass(generateAdapter = true)
data class IncomingKeyRequestInfo(
        override val roomId: String,
        override val sessionId: String,
        override val senderKey: String,
        override val alg: String,
        override val userId: String,
        override val deviceId: String,
        val requestId: String
) : AuditInfo

data class AuditTrail(
        val ageLocalTs: Long,
        val type: TrailType,
        val info: AuditInfo
)
