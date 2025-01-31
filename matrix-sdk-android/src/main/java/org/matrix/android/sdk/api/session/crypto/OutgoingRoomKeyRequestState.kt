/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto

enum class OutgoingRoomKeyRequestState {
    UNSENT,
    SENT,
    SENT_THEN_CANCELED,
    CANCELLATION_PENDING,
    CANCELLATION_PENDING_AND_WILL_RESEND;

    companion object {
        fun pendingStates() = setOf(
                UNSENT,
                CANCELLATION_PENDING_AND_WILL_RESEND,
                CANCELLATION_PENDING
        )
    }
}
