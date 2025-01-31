/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict

/**
 * This class represents the decryption result.
 */
@JsonClass(generateAdapter = true)
data class OlmDecryptionResult(
        /**
         * The decrypted payload (with properties 'type', 'content').
         */
        @Json(name = "payload") val payload: JsonDict? = null,

        /**
         * keys that the sender of the event claims ownership of:
         * map from key type to base64-encoded key.
         */
        @Json(name = "keysClaimed") val keysClaimed: Map<String, String>? = null,

        /**
         * The curve25519 key that the sender of the event is known to have ownership of.
         */
        @Json(name = "senderKey") val senderKey: String? = null,

        /**
         * Devices which forwarded this session to us (normally empty).
         */
        @Json(name = "forwardingCurve25519KeyChain") val forwardingCurve25519KeyChain: List<String>? = null,

        /**
         * True if the key used to decrypt is considered safe (trusted).
         */
        @Json(name = "key_safety") val isSafe: Boolean? = null,
)
