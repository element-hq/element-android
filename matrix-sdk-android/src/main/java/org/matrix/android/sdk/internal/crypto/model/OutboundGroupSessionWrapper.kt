/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import org.matrix.olm.OlmOutboundGroupSession

internal data class OutboundGroupSessionWrapper(
        val outboundGroupSession: OlmOutboundGroupSession,
        val creationTime: Long,
        /**
         * As per MSC 3061, declares if this key could be shared when inviting a new user to the room.
         */
        val sharedHistory: Boolean = false
)
