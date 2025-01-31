/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The type of object we use for importing and exporting megolm session data.
 */
@JsonClass(generateAdapter = true)
internal data class MegolmSessionData(
        /**
         * The algorithm used.
         */
        @Json(name = "algorithm")
        val algorithm: String? = null,

        /**
         * Unique id for the session.
         */
        @Json(name = "session_id")
        val sessionId: String? = null,

        /**
         * Sender's Curve25519 device key.
         */
        @Json(name = "sender_key")
        val senderKey: String? = null,

        /**
         * Room this session is used in.
         */
        @Json(name = "room_id")
        val roomId: String? = null,

        /**
         * Base64'ed key data.
         */
        @Json(name = "session_key")
        val sessionKey: String? = null,

        /**
         * Other keys the sender claims.
         */
        @Json(name = "sender_claimed_keys")
        val senderClaimedKeys: Map<String, String>? = null,

        // This is a shortcut for sender_claimed_keys.get("ed25519")
        // Keep it for compatibility reason.
        @Json(name = "sender_claimed_ed25519_key")
        val senderClaimedEd25519Key: String? = null,

        /**
         * Devices which forwarded this session to us (normally empty).
         */
        @Json(name = "forwarding_curve25519_key_chain")
        val forwardingCurve25519KeyChain: List<String>? = null,

        /**
         * Flag that indicates whether or not the current inboundSession will be shared to
         * invited users to decrypt past messages.
         */
        // When this feature lands in spec name = shared_history should be used
        @Json(name = "org.matrix.msc3061.shared_history")
        val sharedHistory: Boolean = false,
)
