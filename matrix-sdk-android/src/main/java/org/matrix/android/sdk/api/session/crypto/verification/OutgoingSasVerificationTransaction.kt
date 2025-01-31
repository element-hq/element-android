/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.verification

interface OutgoingSasVerificationTransaction : SasVerificationTransaction {
    val uxState: UxState

    enum class UxState {
        UNKNOWN,
        WAIT_FOR_START,
        WAIT_FOR_KEY_AGREEMENT,
        SHOW_SAS,
        WAIT_FOR_VERIFICATION,
        VERIFIED,
        CANCELLED_BY_ME,
        CANCELLED_BY_OTHER
    }
}
