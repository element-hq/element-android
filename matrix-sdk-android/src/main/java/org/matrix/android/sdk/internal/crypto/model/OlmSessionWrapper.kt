/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import kotlinx.coroutines.sync.Mutex
import org.matrix.olm.OlmSession

/**
 * Encapsulate a OlmSession and a last received message Timestamp.
 */
internal data class OlmSessionWrapper(
        // The associated olm session.
        val olmSession: OlmSession,
        // Timestamp at which the session last received a message.
        var lastReceivedMessageTs: Long = 0,

        val mutex: Mutex = Mutex()
) {

    /**
     * Notify that a message has been received on this olm session so that it updates `lastReceivedMessageTs`.
     */
    fun onMessageReceived(currentTimeMillis: Long) {
        lastReceivedMessageTs = currentTimeMillis
    }
}
