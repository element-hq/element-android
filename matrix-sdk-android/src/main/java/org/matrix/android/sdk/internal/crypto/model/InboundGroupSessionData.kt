/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InboundGroupSessionData(

        /** The room in which this session is used. */
        @Json(name = "room_id")
        var roomId: String? = null,

        /** The base64-encoded curve25519 key of the sender. */
        @Json(name = "sender_key")
        var senderKey: String? = null,

        /** Other keys the sender claims. */
        @Json(name = "keys_claimed")
        var keysClaimed: Map<String, String>? = null,

        /** Devices which forwarded this session to us (normally emty). */
        @Json(name = "forwarding_curve25519_key_chain")
        var forwardingCurve25519KeyChain: List<String>? = emptyList(),

        /**
         * Flag that indicates whether or not the current inboundSession will be shared to
         * invited users to decrypt past messages.
         */
        @Json(name = "shared_history")
        val sharedHistory: Boolean = false,

        /**
         * Flag indicating that this key is trusted.
         */
        @Json(name = "trusted")
        val trusted: Boolean? = null,

        )
