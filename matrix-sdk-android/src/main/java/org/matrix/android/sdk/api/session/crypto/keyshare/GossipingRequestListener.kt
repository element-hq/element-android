/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.keyshare

import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.SecretShareRequest

/**
 * Room keys events listener.
 */
interface GossipingRequestListener {
    /**
     * An room key request has been received.
     *
     * @param request the request
     */
    fun onRoomKeyRequest(request: IncomingRoomKeyRequest)

    /**
     * Returns the secret value to be shared.
     * @return true if is handled
     */
    fun onSecretShareRequest(request: SecretShareRequest): Boolean

    /**
     * A room key request cancellation has been received.
     *
     * @param request the cancellation request
     */
    fun onRequestCancelled(request: IncomingRoomKeyRequest)
}
