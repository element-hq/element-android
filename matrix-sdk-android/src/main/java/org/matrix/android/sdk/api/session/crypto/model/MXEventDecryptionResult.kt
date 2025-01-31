/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import org.matrix.android.sdk.api.util.JsonDict

/**
 * The result of a (successful) call to decryptEvent.
 */
data class MXEventDecryptionResult(
        /**
         * The plaintext payload for the event (typically containing "type" and "content" fields).
         */
        val clearEvent: JsonDict,

        /**
         * Key owned by the sender of this event.
         * See MXEvent.senderKey.
         */
        val senderCurve25519Key: String? = null,

        /**
         * Ed25519 key claimed by the sender of this event.
         * See MXEvent.claimedEd25519Key.
         */
        val claimedEd25519Key: String? = null,

        /**
         * List of curve25519 keys involved in telling us about the senderCurve25519Key and
         * claimedEd25519Key. See MXEvent.forwardingCurve25519KeyChain.
         */
        val forwardingCurve25519KeyChain: List<String> = emptyList(),

        val isSafe: Boolean = false
)
