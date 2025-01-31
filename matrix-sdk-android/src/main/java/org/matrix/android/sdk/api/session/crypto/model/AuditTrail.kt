/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode

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

object UnknownInfo : AuditInfo {
    override val roomId: String = ""
    override val sessionId: String = ""
    override val senderKey: String = ""
    override val alg: String = ""
    override val userId: String = ""
    override val deviceId: String = ""
}

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
